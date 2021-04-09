package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.EventsPublisher
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * This component can publish the domain events to an EventsPublisher.
 */
abstract class AbstractProjectionVerticle(
  private val eventsScanner: PgcEventsScanner,
  private val eventsPublisher: EventsPublisher
) : AbstractVerticle() {

  private val log = LoggerFactory.getLogger(eventsScanner.streamName)

  protected fun scanAndPublish(numberOfRows: Int): Future<Long> {
    val promise = Promise.promise<Long>()
    if (log.isDebugEnabled) log.debug("Will scan for new events")
    eventsScanner.scanPendingEvents(numberOfRows)
      .onFailure {
        promise.fail(it)
        log.error("When scanning new events", it)
      }
      .onSuccess { eventsList ->
        if (eventsList.isEmpty()) {
          promise.complete(0)
          return@onSuccess
        }
        if (log.isDebugEnabled) log.debug("Got ${eventsList.size} events")
        GlobalScope.launch(vertx.dispatcher()) {
          try {
            val lastEventPublished = publish(eventsList).await()
            if (lastEventPublished == null) {
              promise.complete(0)
              return@launch
            }
            eventsScanner.updateOffSet(lastEventPublished).await()
            promise.complete(lastEventPublished)
          } catch (e: java.lang.Exception) {
            promise.fail(e)
          }
        }
      }
      .onComplete {
        if (log.isDebugEnabled) log.debug("Scan is now inactive until new request")
      }
    return promise.future()
  }

  suspend fun publish(eventsList: List<EventRecord>): Future<Long?> {
    val promise = Promise.promise<Long?>()
    var lastPublished: Long? = null
    var error = false
    for (event in eventsList) {
      try {
        if (log.isDebugEnabled) log.debug("Will publish ${event.eventId}")
        lastPublished = eventsPublisher.publish(event).await()
      } catch (e: Exception) {
        log.error("When publishing event $event", e)
        if (lastPublished == null) {
          promise.fail(e)
        } else {
          promise.complete(lastPublished)
        }
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
