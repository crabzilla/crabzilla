package io.github.crabzilla.pgc

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * To update customer read model given events
 */
class PgcEventsProjectorVerticle<E : DomainEvent>(
  private val json: Json,
  private val pgPool: PgPool,
  private val eventsProjector: PgcEventsProjector<E>,
  private val endpoint: String
) :
  AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventsProjectorVerticle::class.java)
  }

  override fun start() {
    vertx.eventBus().consumer<JsonObject>(endpoint) { msg ->
      val eventRecord = EventRecord.fromJsonObject(msg.body())
      pgPool.withConnection { conn ->
        val event = DomainEvent.fromJson<E>(json, eventRecord.eventAsjJson.toString())
        eventsProjector.project(conn, event, eventRecord.eventMetadata)
      }
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(true) }
    }
    log.info("Started consuming from endpoint [{}]", endpoint)
  }

  override fun stop() {
    log.info("Stopped consuming from endpoint [{}]", endpoint)
  }
}
