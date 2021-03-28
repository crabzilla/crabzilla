package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventRecord
import io.github.crabzilla.core.EventsPublisher
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
    var lastPublished : Long? = null
    var error = false
    for (event in eventRecords) {
      try {
        eventbus.publish(topic, event)
        if (log.isDebugEnabled) log.debug("Published $event to $topic")
        lastPublished = event.eventId
      } catch (e: Exception) {
        promise.fail(e);
        error = true;
        break
      }
    }
    if (!error) {
      promise.complete(lastPublished)
    }
    return promise.future()
  }
}
