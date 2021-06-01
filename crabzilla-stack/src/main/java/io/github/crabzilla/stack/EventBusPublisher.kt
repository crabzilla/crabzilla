package io.github.crabzilla.stack

import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus

class EventBusPublisher(private val targetEndpoint: String, private val eventbus: EventBus) : EventsPublisher {

  override fun publish(eventRecord: EventRecord): Future<Void> {
    return eventbus.request<Void>(targetEndpoint, eventRecord.toJsonObject()).mapEmpty()
  }
}
