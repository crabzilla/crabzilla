package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.EventsPublisher
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.eventbus.EventBus
import org.slf4j.LoggerFactory

class EventBusEventsPublisher(private val topic: String, private val eventbus: EventBus) : EventsPublisher {

  companion object {
    val log = LoggerFactory.getLogger(EventBusEventsPublisher::class.simpleName)
  }

  override fun publish(eventRecord: EventRecord): Future<Void> {
    val promise = Promise.promise<Void>()
    eventbus.request<Boolean>(topic, eventRecord.toJsonObject()) {
      if (it.failed()) {
        promise.fail(it.cause())
      } else {
        promise.complete()
      }
    }
    return promise.future()
  }
}
