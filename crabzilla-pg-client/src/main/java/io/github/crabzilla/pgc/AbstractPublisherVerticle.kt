package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventsPublisher
import io.vertx.core.AbstractVerticle
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

  protected fun scanAndPublish(numberOfRows: Int) {
    if (isScanning.get()) {
      if (log.isDebugEnabled) log.debug("Will skip scan since it is already running")
      return
    }
    if (log.isDebugEnabled) log.debug("Will scan for new events")
    isScanning.set(true)
    eventsScanner.scanPendingEvents(numberOfRows)
      .onFailure { log.error("When scanning new events", it) }
      .onSuccess { eventsList ->
        if (eventsList.isEmpty()) {
          return@onSuccess
        }
        log.info("Got ${eventsList.size} events")
        eventsPublisher.publish(eventsList)
          .onFailure { log.error("When publishing events", it) }
          .onSuccess { lastEventPublished ->
            if (lastEventPublished != null) {
              eventsScanner.updateOffSet(lastEventPublished)
                .onFailure { log.error("When updating offset to $lastEventPublished", it) }
                .onSuccess { log.info("Offset updated to $lastEventPublished") }
            }
          }
      }
      .onComplete {
        isScanning.set(false)
        if (log.isDebugEnabled) log.debug("Scan is now inactive until new request")
      }
  }
}
