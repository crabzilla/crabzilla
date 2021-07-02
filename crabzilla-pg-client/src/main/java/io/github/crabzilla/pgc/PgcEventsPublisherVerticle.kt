package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventBusPublisher
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiFunction
import kotlin.math.min

/**
 * This component will publish the domain events to an EventsPublisher.
 */
class PgcEventsPublisherVerticle : PgcAbstractVerticle() {

  companion object {
    // TODO this must be by instance
    const val PUBLISHER_ENDPOINT = "publisher.verticle" // TODO add endpoint for pause, resume, restart from N, etc
    const val PUBLISHER_RESCHEDULED_ENDPOINT = "publisher.verticle.rescheduled"
  }

  private val log = LoggerFactory.getLogger(PgcEventsPublisherVerticle::class.java)

  private val action: Handler<Long?> = handler()
  private val failures = AtomicLong(0L)
  private val showStats = AtomicBoolean(true)

  lateinit var scanner: PgcEventsScanner
  lateinit var publisher: EventBusPublisher
  private lateinit var options: Config

  override fun start() {

    val config = Config.create(config())
    log.info("Config {}", config().encodePrettily())

    val sqlClient = sqlClient(config())
    scanner = PgcEventsScanner(sqlClient, config.projectionId)
    publisher = EventBusPublisher(config.targetEndpoint, vertx.eventBus())
    options = Config.create(config())

    // Schedule the first execution
    vertx.setTimer(options.interval, action)
    vertx.setPeriodic(options.statsInterval) {
      showStats.set(true)
    }
    // force scan endpoint
    vertx.eventBus().consumer<Void>(PUBLISHER_ENDPOINT) { msg ->
      log.info("Forced scan")
      scanAndPublish(options.maxNumberOfRows)
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
    log.info("Started pooling events with {}", options)
  }

  override fun stop() {
    log.info("Stopped")
  }

  private fun handler(): Handler<Long?> {
    fun registerFailure() {
      val nextInterval = min(options.maxInterval, options.interval * failures.incrementAndGet())
      vertx.eventBus().send(PUBLISHER_RESCHEDULED_ENDPOINT, nextInterval)
    }
    fun registerSuccessOrStillBusy() {
      failures.set(0)
      vertx.eventBus().send(PUBLISHER_RESCHEDULED_ENDPOINT, options.interval)
    }
    return Handler { _ ->
      scanAndPublish(options.maxNumberOfRows)
        .onFailure {
          log.error("When scanning for new events", it)
          registerFailure()
        }
        .onSuccess {
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
              registerSuccessOrStillBusy()
            }
          }
        }
    }
  }

  private fun scanAndPublish(numberOfRows: Int): Future<Long> {
    fun publish(eventsList: List<EventRecord>): Future<Long> {
      fun action(eventRecord: EventRecord): Future<Void> {
        return publisher.publish(eventRecord).mapEmpty()
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
      val eventSequence = AtomicLong(0)
      val initialFuture = Future.succeededFuture<Void>()
      foldLeft(
        eventsList.iterator(), initialFuture,
        { currentFuture: Future<Void>, eventRecord: EventRecord ->
          currentFuture.onSuccess {
            log.trace("Successfully projected event #{}", eventRecord.eventMetadata.eventId)
            eventSequence.set(eventRecord.eventMetadata.eventSequence)
            action(eventRecord)
          }.onFailure {
            log.debug(
              "Skipped {} since the latest successful event was {}",
              eventRecord.eventMetadata.eventId, eventSequence
            )
          }
        }
      ).onComplete {
        promise.complete(eventSequence.get())
      }
      return promise.future()
    }
    val promise = Promise.promise<Long>()
    scanner.scanPendingEvents(numberOfRows)
      .onFailure {
        promise.fail(it)
        log.error("When scanning new events", it)
      }
      .onSuccess { eventsList ->
        if (eventsList.isEmpty()) {
          promise.complete(0)
          return@onSuccess
        }
        log.debug("Found {} new events. The last one is #{}", eventsList.size, eventsList.last().eventMetadata.eventId)
        publish(eventsList)
          .onFailure { promise.fail(it) }
          .onSuccess { lastEventPublished ->
            log.debug("After publishing {}, the latest published event id is #{}", eventsList.size, lastEventPublished)
            if (lastEventPublished == 0L) {
              promise.complete(0L)
            } else {
              scanner.updateOffSet(lastEventPublished)
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
        log.trace("Scan is now inactive until new request")
      }
    return promise.future()
  }

  private class Config(
    val projectionId: String,
    val targetEndpoint: String,
    val interval: Long,
    val maxNumberOfRows: Int,
    val maxInterval: Long,
    val statsInterval: Long
  ) {
    companion object {
      fun create(config: JsonObject): Config {
        val projectionId = config.getString("projectionId")
        val targetEndpoint = config.getString("targetEndpoint")
        val interval = config.getLong("interval", 500)
        val maxNumberOfRows = config.getInteger("maxNumberOfRows", 500)
        val maxInterval = config.getLong("maxInterval", 60_000)
        val statsInterval = config.getLong("statsInterval", 30_000)
        return Config(projectionId, targetEndpoint, interval, maxNumberOfRows, maxInterval, statsInterval)
      }
    }
  }
}
