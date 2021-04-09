package io.github.crabzilla.pgc

import io.vertx.junit5.VertxExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
internal class EventBusEventsPublisherTest {

  private val topic = "example1"

//  @Test
//  @DisplayName("Should publish 5 events")
//  fun s1(vertx: Vertx, tc: VertxTestContext) {
//    val eventbus = vertx.eventBus()
//    val publisher = EventBusEventsPublisher(topic, eventbus)
//    val records = generateSequence(1) { it + 1 }
//      .take(5)
//      .map { EventRecord("Customer", it, JsonObject(), it.toLong()) }
//      .toList()
//    publisher.publish(records)
//      .onFailure { tc.failNow(it) }
//      .onSuccess { lastEventId ->
//        assertThat(lastEventId).isEqualTo(5)
//        tc.completeNow()
//      }
//  }
//
//  @Test
//  @DisplayName("Should return null when publishing an empty list of events")
//  fun s2(vertx: Vertx, tc: VertxTestContext) {
//
//    val eventbus = vertx.eventBus()
//    val publisher = EventBusEventsPublisher(topic, eventbus)
//
//    publisher.publish(emptyList())
//      .onFailure { tc.failNow(it) }
//      .onSuccess { lastEventId ->
//        if (lastEventId == null) {
//          tc.completeNow()
//        } else {
//          tc.failNow("Should be null")
//        }
//      }
//  }
}
