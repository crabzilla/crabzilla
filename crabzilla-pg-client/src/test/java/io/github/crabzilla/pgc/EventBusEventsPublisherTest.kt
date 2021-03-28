package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventRecord
import io.mockk.mockk
import io.mockk.verifyOrder
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Test

internal class EventBusEventsPublisherTest {

  private val topic = "example1"

  @Test
  fun it_should_publish() {

    val eventbus = mockk<EventBus>(relaxed = true)
    val publisher = EventBusEventsPublisher(topic, eventbus)

    val records = generateSequence(0) { it + 1 }
      .take(5)
      .map { EventRecord("Customer", it, JsonObject(), it.toLong()) }
      .toList()

    publisher.publish(records)

    verifyOrder {
      records.forEach { r ->
        eventbus.publish(topic, r)
      }
    }
  }
}
