package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventsPublisher
import org.slf4j.LoggerFactory

/**
 * This component will be triggered using a Vertx periodic task. Then it can publish the domain events to
 * a EventsPublisher.
 */
class PgcPoolingPublisherVerticle(
  eventsScanner: PgcEventsScanner,
  eventsPublisher: EventsPublisher,
  private val intervalInMilliseconds: Long = MILLISECONDS,
  private val numberOfRows: Int = NUMBER_OF_ROWS

) : AbstractPublisherVerticle(eventsScanner, eventsPublisher) {

  companion object {
    private val log = LoggerFactory.getLogger(PgcPoolingPublisherVerticle::class.java)
    const val PUBLISHER_ENDPOINT = "publisher.verticle"
    private const val MILLISECONDS = 1000L
    private const val NUMBER_OF_ROWS = 1000
  }

  override fun start() {
    val eventbus = vertx.eventBus()
    vertx.setPeriodic(intervalInMilliseconds) { tick: Long ->
      if (log.isDebugEnabled) log.debug("Received a notification #$tick")
      scanAndPublish(numberOfRows)
        .onFailure { log.error("When scanning for new events", it) }
        .onSuccess { if (log.isDebugEnabled) log.debug("$it events were scanned") }
    }
    eventbus.consumer<Int>(PUBLISHER_ENDPOINT) { msg ->
      scanAndPublish(msg.body())
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(it) }
    }
    log.info("Started pooling for at most $numberOfRows rows each $intervalInMilliseconds milliseconds")
  }

  override fun stop() {
    log.info("Stopped")
  }
}
