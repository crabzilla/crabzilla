package io.github.crabzilla.stack

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiFunction
import kotlin.math.min

/**
 * This component will publish the domain events to an EventsPublisher.
 */
class EventsPublisherVerticle(
  private val eventsScanner: EventsScanner,
  private val eventsPublisher: EventsPublisher,
  private val intervalInMilliseconds: Long = DEFAULT_INTERVAL,
  private val numberOfRows: Int = DEFAULT_NUMBER_OF_ROWS

) : AbstractVerticle() {

  companion object {
    const val PUBLISHER_ENDPOINT = "publisher.verticle" // TODO add endpoint for pause, resume, restart from N, etc
    const val PUBLISHER_RESCHEDULED_ENDPOINT = "publisher.verticle.rescheduled"
    private const val DEFAULT_INTERVAL = 500L
    private const val DEFAULT_NUMBER_OF_ROWS = 500
    private const val DEFAULT_MAX_INTERVAL = 60_000L
  }

  private val log = LoggerFactory.getLogger(eventsScanner.streamName())

  private val action: Handler<Long?> = handler()
  private val failures = AtomicLong(0L)
  private val showStats = AtomicBoolean(true)

  override fun start() {
    // Schedule the first execution
    vertx.setTimer(intervalInMilliseconds, action)
    vertx.setPeriodic(DEFAULT_MAX_INTERVAL) {
      log.info("* ")
      showStats.set(true)
    }

    // force scan endpoint
    vertx.eventBus().consumer<Void>(PUBLISHER_ENDPOINT) { msg ->
      log.info("Forced scan")
      scanAndPublish(numberOfRows)
        .onFailure { msg.fail(500, it.message) }
        .onSuccess {
          log.info("Finished scan")
          msg.reply(true)
        }
    }
    vertx.eventBus().consumer<Long>(PUBLISHER_RESCHEDULED_ENDPOINT) { msg ->
      val nextInterval = msg.body()
      vertx.setTimer(nextInterval, action)
      log.debug("Rescheduled to next {} milliseconds", nextInterval)
    }
    log.info("Started pooling for at most {} rows each {} milliseconds", numberOfRows, intervalInMilliseconds)
  }

  override fun stop() {
    log.info("Stopped")
  }

  private fun handler(): Handler<Long?> {
    fun registerFailure() {
      val nextInterval = min(DEFAULT_MAX_INTERVAL, intervalInMilliseconds * failures.incrementAndGet())
      vertx.eventBus().send(PUBLISHER_RESCHEDULED_ENDPOINT, nextInterval)
    }
    fun registerSuccessOrStillBusy() {
      failures.set(0)
      vertx.eventBus().send(PUBLISHER_RESCHEDULED_ENDPOINT, intervalInMilliseconds)
    }
    return Handler { tick ->
      log.debug("Tick $tick")
      scanAndPublish(numberOfRows)
        .onFailure {
          log.error("When scanning for new events", it)
          registerFailure()
        }
        .onSuccess {
          log.debug("$it events were scanned")
          when (it) {
            -1L -> {
              log.debug("Still busy")
              registerSuccessOrStillBusy()
            }
            0L -> {
              log.debug("No new events")
              registerFailure()
            }
            else -> {
              log.debug("Found {} events", it)
              registerSuccessOrStillBusy()
            }
          }
        }
    }
  }

  private fun scanAndPublish(numberOfRows: Int): Future<Long> {
    fun publish(eventsList: List<EventRecord>): Future<Long> {
      fun action(eventRecord: EventRecord): Future<Void> {
        return eventsPublisher.publish(eventRecord).mapEmpty()
      }
      fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
        var result = identity
        while (iterator.hasNext()) {
          val next = iterator.next()
          result = bf.apply(result, next)
        }
        return result
      }
      val promise = Promise.promise<Long>()
      val eventId = AtomicLong(0)
      val initialFuture = Future.succeededFuture<Void>()
      foldLeft(
        eventsList.iterator(), initialFuture,
        { currentFuture: Future<Void>, eventRecord: EventRecord ->
          currentFuture.onSuccess {
            log.debug("Successfully projected {}", eventRecord.eventId)
            eventId.set(eventRecord.eventId)
            action(eventRecord)
          }.onFailure {
            log.debug("Skipped {} since the latest successful event was {}", eventRecord.eventId, eventId)
          }
        }
      ).onComplete {
        promise.complete(eventId.get())
      }
      return promise.future()
    }
    val promise = Promise.promise<Long>()
    log.debug("Will scan for new events")
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
        log.debug("Got ${eventsList.size} events")
        publish(eventsList)
          .onFailure { promise.fail(it) }
          .onSuccess { lastEventPublished ->
            log.debug("After publishing {}, the latest published event id is {}", eventsList.size, lastEventPublished)
            if (lastEventPublished == 0L) {
              promise.complete(0L)
            } else {
              eventsScanner.updateOffSet(lastEventPublished)
                .onFailure { promise.fail(it) }
                .onSuccess {
                  if (showStats.get()) {
                    log.info("Updated latest offset to {}", lastEventPublished)
                    showStats.set(false)
                  }
                  promise.complete(lastEventPublished)
                }
            }
          }
      }
      .onComplete {
        log.debug("Scan is now inactive until new request")
      }
    return promise.future()
  }
}
