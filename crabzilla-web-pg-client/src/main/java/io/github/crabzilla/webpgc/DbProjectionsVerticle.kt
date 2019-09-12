package io.github.crabzilla.webpgc

import io.github.crabzilla.EventBusChannels
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.EntityJsonAware
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcUowProjector
import io.github.crabzilla.pgc.readModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

abstract class DbProjectionsVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(DbProjectionsVerticle::class.java)
    val processId: String = ManagementFactory.getRuntimeMXBean().name // TODO does this work with AOT?
  }

  private val readDb : PgPool by lazy { readModelPgPool(vertx, config()) }
  private val jsonFunctions: MutableMap<String, EntityJsonAware<out Entity>> = mutableMapOf()

  override fun start() {
    val implClazz = this::class.java.name
    vertx.eventBus().consumer<String>(implClazz) { msg ->
      log.info("received " + msg.body())
      msg.reply("$implClazz is already running here: $processId")
    }
  }

  fun addEntityJsonAware(entityName: String, jsonAware: EntityJsonAware<out Entity>) {
    jsonFunctions[entityName] = jsonAware
  }

  fun addProjector(projectionName: String, projector: PgcEventProjector) {
    log.info("adding projector for $projectionName subscribing on ${EventBusChannels.unitOfWorkChannel}")
    val uolProjector = PgcUowProjector(readDb, projectionName)
    vertx.eventBus().consumer<String>(EventBusChannels.unitOfWorkChannel) { message ->
      val uowEvents = toUnitOfWorkEvents(JsonObject(message.body()), jsonFunctions)
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

}
