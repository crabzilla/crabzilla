package io.github.crabzilla.webpgc

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.Entity
import io.github.crabzilla.EntityJsonAware
import io.github.crabzilla.UnitOfWork.JsonMetadata
import io.github.crabzilla.UnitOfWorkEvents
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcUowProjector
import io.github.crabzilla.pgc.readModelPgPool
import io.reactiverse.pgclient.PgPool
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

abstract class DbProjectionsVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(DbProjectionsVerticle::class.java)
    val processId: String = ManagementFactory.getRuntimeMXBean().name // TODO does this work with AOT?
    fun amIAlreadyRunning(projectionEndpoint: String) : String {
      return "$projectionEndpoint-ping"
    }
  }

  private val projectionEndpoint: String by lazy { config().getString("PROJECTION_ENDPOINT") }
  private val readDb : PgPool by lazy { readModelPgPool(vertx, config()) }
  private val jsonFunctions: MutableMap<String, EntityJsonAware<out Entity>> = mutableMapOf()

  override fun start(startFuture: Future<Void>) {
    vertx.eventBus().consumer<String>(amIAlreadyRunning(projectionEndpoint)) { msg ->
      log.info("received " + msg.body())
      msg.reply("Yes, $projectionEndpoint is already running here: $processId")
    }
  }

  fun addEntityJsonAware(entityName: String, jsonAware: EntityJsonAware<out Entity>) {
    jsonFunctions[entityName] = jsonAware
  }

  fun addProjector(projectionName: String, projector: PgcEventProjector) {
    log.info("adding projector for $projectionName subscribing on $projectionEndpoint")
    val uolProjector = PgcUowProjector(readDb, projectionName)
    vertx.eventBus().consumer<String>(projectionEndpoint) { message ->
      val uowEvents = toUnitOfWOrkEvents(JsonObject(message.body()))
      if (uowEvents == null) {
        log.error("Cannot send these events to be projected. Check if all entities have a jsonAware.")
      } else {
        uolProjector.handle(uowEvents, projector, Handler { result ->
          if (result.failed()) { // TODO circuit breaker
            log.error("Projection [$projectionName] failed: " + result.cause().message)
          }
        })
      }
    }
  }

  private fun toUnitOfWOrkEvents(json: JsonObject): UnitOfWorkEvents? {

    val uowId = json.getLong("uowId")
    val entityName = json.getString(JsonMetadata.ENTITY_NAME)
    val entityId = json.getInteger(JsonMetadata.ENTITY_ID)
    val eventsArray = json.getJsonArray(JsonMetadata.EVENTS)

    val jsonAware = jsonFunctions[entityName]
    if (jsonAware == null) {
      log.error("JsonAware for $entityName wasn't found")
      return null
    }

    val jsonToEventPair: (Int) -> Pair<String, DomainEvent> = { index ->
      val jsonObject = eventsArray.getJsonObject(index)
      val eventName = jsonObject.getString(JsonMetadata.EVENT_NAME)
      val eventJson = jsonObject.getJsonObject(JsonMetadata.EVENTS_JSON_CONTENT)
      val domainEvent = jsonAware.eventFromJson(eventName, eventJson)
      domainEvent
    }

    val events: List<Pair<String, DomainEvent>> = List(eventsArray.size(), jsonToEventPair)
    return UnitOfWorkEvents(uowId, entityId, events)

  }

}
