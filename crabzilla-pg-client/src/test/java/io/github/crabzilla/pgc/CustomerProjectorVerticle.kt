package io.github.crabzilla.pgc

import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.CustomerRepository
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * To update customer read model given events
 */
class CustomerProjectorVerticle(private val json: Json, private val repo: CustomerRepository) :
  AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerProjectorVerticle::class.java)
    const val topic = "customers"
  }

  override fun start() {
    vertx.eventBus().consumer<JsonObject>(topic) { msg ->
      val eventRecord = EventRecord.fromJsonObject(msg.body())
      publish(eventRecord)
        .onFailure { msg.fail(500, it.message) }
        .onSuccess {
          log.info("Projected $eventRecord")
          msg.reply(true)
        }
    }
    log.info("Started consuming from topic [$topic]")
  }

  override fun stop() {
    log.info("Stopped")
  }

  private fun publish(eventRecord: EventRecord): Future<Void> {
    log.info("Will project $eventRecord")
    val event = json.decodeFromString(DOMAIN_EVENT_SERIALIZER, eventRecord.eventAsjJson.toString()) as CustomerEvent
    log.info("The event is $event")
    return when (event) {
      is CustomerEvent.CustomerRegistered -> repo.upsert(eventRecord.aggregateId, event.name, false)
      is CustomerEvent.CustomerActivated -> repo.updateStatus(eventRecord.aggregateId, true)
      is CustomerEvent.CustomerDeactivated -> repo.updateStatus(eventRecord.aggregateId, false)
    }
  }
}
