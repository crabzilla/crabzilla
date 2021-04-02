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

  override fun publish(eventRecords: List<EventRecord>): Future<Long> {
    val promise = Promise.promise<Long>()
    var lastPublished: Long? = null
    var error = false
    for (event in eventRecords) {
      try {
        eventbus.publish(topic, event.toJsonObject())
        if (log.isDebugEnabled) log.debug("Published $event to $topic")
        lastPublished = event.eventId
      } catch (e: Exception) {
        log.error("When publishing $event", e)
        promise.fail(e)
        error = true
        break
      }
    }
    if (!error) {
      promise.complete(lastPublished)
    }
    return promise.future()
  }
}
