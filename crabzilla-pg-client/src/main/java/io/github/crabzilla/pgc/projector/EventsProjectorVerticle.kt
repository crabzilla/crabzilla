package io.github.crabzilla.pgc.projector

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.PgcAbstractVerticle
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * A broker verticle to project events to database
 */
class EventsProjectorVerticle : PgcAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorVerticle::class.java)
  }

  private lateinit var targetEndpoint: String

  override fun start() {

    targetEndpoint = config().getString("targetEndpoint")

    val json = json(config())
    val pgPool = pgPool(config())
    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    val eventsProjector = provider!!.create()

    vertx.eventBus().consumer<JsonObject>(targetEndpoint) { msg ->
      val eventRecord = EventRecord.fromJsonObject(msg.body())
      pgPool.withConnection { conn ->
        val event = DomainEvent.fromJson<DomainEvent>(json, eventRecord.eventAsjJson.toString())
        eventsProjector.project(conn, event, eventRecord.eventMetadata)
//          .compose {
//            // TODO update projection offset within the same transaction
//            Future.succeededFuture<Void>()
//          }
      }
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(true) }
    }
    log.info("Started consuming from endpoint [{}]", targetEndpoint)
  }

  override fun stop() {
    log.info("Stopped consuming from endpoint [{}]", targetEndpoint)
  }
}
