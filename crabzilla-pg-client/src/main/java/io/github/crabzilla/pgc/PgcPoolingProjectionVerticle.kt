package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.EventsPublisher
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiFunction
import kotlin.math.min

/**
 * This component will be triggered using a Vertx periodic task. Then it can publish the domain events to
 * an EventsPublisher.
 */
class PgcPoolingProjectionVerticle(
  private val eventsScanner: PgcEventsScanner,
  private val eventsPublisher: EventsPublisher,
  private val intervalInMilliseconds: Long = DEFAULT_INTERVAL,
  private val numberOfRows: Int = DEFAULT_NUMBER_OF_ROWS

) : AbstractVerticle() {

  companion object {
    const val PUBLISHER_ENDPOINT = "publisher.verticle" // TODo add endpoint for pause, resume, restart from N, etc
    const val PUBLISHER_RESCHEDULED_ENDPOINT = "publisher.verticle.rescheduled"
    private const val DEFAULT_INTERVAL = 100L
    private const val DEFAULT_NUMBER_OF_ROWS = 100
    private const val DEFAULT_MAX_INTERVAL = 10_000L
  }

  private val log = LoggerFactory.getLogger(eventsScanner.streamName)

  private val action: Handler<Long?> = handler()
  private val failures = AtomicLong(0L)

  override fun start() {
    // force scan endpoint
    vertx.eventBus().consumer<Void>(PUBLISHER_ENDPOINT) { msg ->
      log.info("Forced scan")
      val id = handler().handle(0L)
      log.info("Projected until $id")
      // msg.reply(id)
    }
    vertx.eventBus().consumer<Long>(PUBLISHER_RESCHEDULED_ENDPOINT) { msg ->
      val nextInterval = msg.body()
      log.info("Rescheduled to next $nextInterval milliseconds")
    }
    log.info("Started pooling for at most $numberOfRows rows each $intervalInMilliseconds milliseconds")
  }

  override fun stop() {
    log.info("Stopped")
  }

  fun handler(): Handler<Long?> {
    fun publish(eventsList: List<EventRecord>): Future<Long> {
      fun action(eventRecord: EventRecord): Future<Boolean> {
        val promise = Promise.promise<Boolean>()
        eventsPublisher.publish(eventRecord)
          .onSuccess { promise.complete(true) }
          .onFailure {
            log.error("When projecting event $eventRecord", it)
            promise.complete(false)
          }
        return promise.future()
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
      val initialFuture = Future.succeededFuture<Boolean>(true)
      foldLeft(
        eventsList.iterator(), initialFuture,
        { currentFuture: Future<Boolean>,
          eventRecord: EventRecord ->
          currentFuture.compose { successful: Boolean ->
            if (successful) {
              log.info("Successfully projected ${eventRecord.eventId}")
              eventId.set(eventRecord.eventId)
              action(eventRecord)
            } else {
              log.info("Skipped ${eventRecord.eventId} since the latest successful event was $eventId")
              Future.failedFuture("The latest successful event was $eventId")
            }
          }
        }
      ).onComplete {
        promise.complete(eventId.get())
      }
      return promise.future()
    }
    fun registerFailure() {
      val nextInterval = min(DEFAULT_MAX_INTERVAL, intervalInMilliseconds * failures.incrementAndGet())
      vertx.eventBus().send(PUBLISHER_RESCHEDULED_ENDPOINT, nextInterval)
    }
    fun registerSuccess() {
      failures.set(0)
      vertx.eventBus().send(PUBLISHER_RESCHEDULED_ENDPOINT, intervalInMilliseconds)
    }
    fun scanAndPublish(numberOfRows: Int): Future<Long> {
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
          publish(eventsList)
            .onFailure { promise.fail(it) }
            .onSuccess { lastEventPublished ->
              log.info("After publishing  ${eventsList.size}, the latest published event id is $lastEventPublished")
              if (lastEventPublished == 0L) {
                promise.complete(0L)
                return@onSuccess
              }
              eventsScanner.updateOffSet(lastEventPublished)
                .onFailure { promise.fail(it) }
                .onSuccess {
                  log.info("Updated latest offset to $lastEventPublished")
                  promise.complete(lastEventPublished)
                }
            }
        }
        .onComplete {
          if (log.isDebugEnabled) log.debug("Scan is now inactive until new request")
        }
      return promise.future()
    }
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
}
