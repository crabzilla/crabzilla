package io.github.crabzilla.example1

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
class CustomerEventsProjectorVerticle(private val json: Json, private val pgPool: PgPool) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerEventsProjectorVerticle::class.java)
    const val topic = "customers"
  }

  override fun start() {
    vertx.eventBus().consumer<JsonObject>(topic) { msg ->
      val eventRecord = EventRecord.fromJsonObject(msg.body())
      pgPool.withConnection { conn ->
        val event = DomainEvent.fromJson<CustomerEvent>(json, eventRecord.eventAsjJson.toString())
        CustomerEventsProjector.project(conn, event, eventRecord.eventMetadata)
      }
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(true) }
    }
    log.info("Started consuming from topic [$topic]")
  }

  override fun stop() {
    log.info("Stopped")
  }
}
