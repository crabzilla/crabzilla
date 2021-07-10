package io.github.crabzilla.pgc.publisher

import io.github.crabzilla.pgc.PgcAbstractVerticle
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.foldLeft
import io.github.crabzilla.stack.publisher.EventBusPublisher
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * This component will publish the domain events to an EventsPublisher.
 */
class EventsPublisherVerticle : PgcAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsPublisherVerticle::class.java)
    // TODO this must be by instance: add endpoint for pause, resume, restart from N, etc
  }

  private val action: Handler<Long?> = handler()
  private val failures = AtomicLong(0L)
  private val showStats = AtomicBoolean(true)

  private lateinit var options: Config
  lateinit var scanner: PgcEventsScanner
  lateinit var publisher: EventBusPublisher

  override fun start() {

    options = Config.create(config())

    val sqlClient = sqlClient(config())
    scanner = PgcEventsScanner(sqlClient, options.projectionId)
    publisher = EventBusPublisher(options.targetEndpoint, vertx.eventBus())
    options = Config.create(config())

    // Schedule the first execution
    vertx.setTimer(options.interval, action)
    vertx.setPeriodic(options.statsInterval) {
      showStats.set(true)
    }

    log.info("Started pooling events with {}", options)
  }

  override fun stop() {
    log.info("Stopped")
  }

  private fun handler(): Handler<Long?> {
    fun registerFailure() {
      val nextInterval = min(options.maxInterval, options.interval * failures.incrementAndGet())
      vertx.setTimer(nextInterval, action)
      log.debug("Rescheduled to next {} milliseconds", nextInterval)
    }
    fun registerSuccessOrStillBusy() {
      failures.set(0)
      vertx.setTimer(options.interval, action)
      log.debug("Rescheduled to next {} milliseconds", options.interval)
    }
    return Handler { _ ->
      scanAndPublish(options.maxNumberOfRows)
        .compose { retrievedRows ->
          when (retrievedRows) {
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
          Future.succeededFuture(retrievedRows)
        }
    }
  }

  private fun scanAndPublish(numberOfRows: Int): Future<Long> {
    fun publish(eventsList: List<EventRecord>): Future<Long> {
      fun action(eventRecord: EventRecord): Future<Void> {
        return publisher.publish(eventRecord).mapEmpty()
      }
      val promise = Promise.promise<Long>()
      val eventSequence = AtomicLong(0)
      val initialFuture = Future.succeededFuture<Void>()
      foldLeft(
        eventsList.iterator(), initialFuture
      ) { currentFuture: Future<Void>, eventRecord: EventRecord ->
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
      }.onComplete {
        promise.complete(eventSequence.get())
      }
      return promise.future()
    }

    return scanner.scanPendingEvents(numberOfRows)
      .compose { eventsList ->
        log.debug("Found {} new events. The last one is #{}", eventsList.size, eventsList.last().eventMetadata.eventId)
        publish(eventsList)
          .onSuccess { lastEventPublished ->
            log.debug("After publishing {}, the latest published event id is #{}", eventsList.size, lastEventPublished)
            if (lastEventPublished == 0L) {
              Future.succeededFuture(0L)
            } else {
              scanner.updateOffSet(lastEventPublished)
                .compose {
                  if (showStats.get()) {
                    log.info("Updated latest offset to {}", lastEventPublished)
                    showStats.set(false)
                  }
                  Future.succeededFuture(lastEventPublished)
                }
            }
          }
      }
  }

  private data class Config(
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
