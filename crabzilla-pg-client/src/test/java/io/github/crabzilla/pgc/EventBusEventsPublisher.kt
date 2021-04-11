package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.EventsPublisher
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import org.slf4j.LoggerFactory

class EventBusEventsPublisher(private val topic: String, private val eventbus: EventBus) : EventsPublisher {

  companion object {
    val log = LoggerFactory.getLogger(EventBusEventsPublisher::class.simpleName)
  }

  override fun publish(eventRecord: EventRecord): Future<Void> {
    eventbus.publish(topic, eventRecord.toJsonObject())
    return Future.succeededFuture()
  }
}
