package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventsPublisher
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This component will be triggered using a Vertx periodic task. Then it can publish the domain events to
 * a EventsPublisher.
 */
abstract class AbstractPublisherVerticle(
  private val eventsScanner: PgcEventsScanner,
  private val eventsPublisher: EventsPublisher
) : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(AbstractPublisherVerticle::class.java)
  }

  private val isScanning = AtomicBoolean(false)

  protected fun scanAndPublish(numberOfRows: Int): Future<Long> {
    val promise = Promise.promise<Long>()
    if (isScanning.get()) {
      if (log.isDebugEnabled) log.debug("Will skip scan since it is already running")
      promise.complete(-1)
      return promise.future()
    }
    if (log.isDebugEnabled) log.debug("Will scan for new events")
    isScanning.set(true)
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
        eventsPublisher.publish(eventsList)
          .onFailure {
            promise.fail(it)
            log.error("When publishing events", it)
          }
          .onSuccess { lastEventPublished ->
            if (lastEventPublished == null) {
              promise.complete(0)
              return@onSuccess
            }
            eventsScanner.updateOffSet(lastEventPublished)
              .onFailure {
                promise.fail(it)
                log.error("When updating offset to $lastEventPublished", it)
              }
              .onSuccess {
                promise.complete(eventsList.last().eventId)
                if (log.isDebugEnabled) log.debug("Offset updated to $lastEventPublished")
              }
          }
      }
      .onComplete {
        isScanning.set(false)
        if (log.isDebugEnabled) log.debug("Scan is now inactive until new request")
      }
    return promise.future()
  }
}
