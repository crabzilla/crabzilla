package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventsPublisher
import io.vertx.core.Handler
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * This component will be triggered using a Vertx periodic task. Then it can publish the domain events to
 * an EventsPublisher.
 */
class PgcPoolingProjectionVerticle(
  eventsScanner: PgcEventsScanner,
  eventsPublisher: EventsPublisher,
  private val intervalInMilliseconds: Long = DEFAULT_INTERVAL,
  private val numberOfRows: Int = DEFAULT_NUMBER_OF_ROWS

) : AbstractProjectionVerticle(eventsScanner, eventsPublisher) {

  companion object {
    const val PUBLISHER_ENDPOINT = "publisher.verticle"
    private const val DEFAULT_INTERVAL = 1_000L
    private const val DEFAULT_NUMBER_OF_ROWS = 1_000
    private const val DEFAULT_MAX_INTERVAL = 10_000L
  }

  private val log = LoggerFactory.getLogger(eventsScanner.streamName)

  private val action = handler()
  private val failures = AtomicLong(0)

  override fun start() {
    val eventbus = vertx.eventBus()
    // Schedule the first execution
    vertx.setTimer(intervalInMilliseconds, action)
    eventbus.consumer<Int>(PUBLISHER_ENDPOINT) { msg ->
      scanAndPublish(msg.body())
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(it) }
    }
    log.info("Started pooling for at most $numberOfRows rows each $intervalInMilliseconds milliseconds")
  }

  fun handler(): Handler<Long?> {
    return Handler { tick ->
      if (log.isDebugEnabled) log.debug("Tick $tick")
      scanAndPublish(numberOfRows)
        .onFailure {
          log.error("When scanning for new events", it)
          registerFailure()
        }
        .onSuccess {
          if (log.isDebugEnabled) log.debug("$it events were scanned")
          when (it) {
            -1L -> {
              if (log.isDebugEnabled) log.debug("Still busy")
              registerFailure()
            }
            0L -> {
              if (log.isDebugEnabled) log.debug("No new events")
              registerFailure()
            }
            else -> {
              if (log.isDebugEnabled) log.debug("Found $it events")
              registerSuccess()
            }
          }
        }
    }
  }

  private fun registerFailure() {
    val nextInterval = min(DEFAULT_MAX_INTERVAL, intervalInMilliseconds * failures.incrementAndGet())
    vertx.setTimer(nextInterval, action)
    if (log.isDebugEnabled) log.debug("Rescheduled")
  }

  private fun registerSuccess() {
    failures.set(0)
    vertx.setTimer(intervalInMilliseconds, action)
    if (log.isDebugEnabled) log.debug("Rescheduled")
  }

  override fun stop() {
    log.info("Stopped")
  }
}
