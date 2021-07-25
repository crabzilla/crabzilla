package io.github.crabzilla.pgc.publisher

import io.github.crabzilla.pgc.PgcAbstractVerticle
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.foldLeft
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class EventsPublisherVerticle : PgcAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsPublisherVerticle::class.java)

    // TODO this must be by instance: add endpoint for pause, resume, restart from N, etc
  }

  private val action: Handler<Long?> = handler()
  private val failures = AtomicLong(0L)
  private val showStats = AtomicBoolean(true)

  private lateinit var options: Config
  lateinit var scanner: EventsScanner

  override fun start() {

    options = Config.create(config())

    val sqlClient = sqlClient(config())
    scanner = EventsScanner(sqlClient, options.projectionId)
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
        return vertx.eventBus().request<Void>(options.targetEndpoint, eventRecord.toJsonObject()).mapEmpty()
      }
      val eventSequence = AtomicLong(0)
      val initialFuture = Future.succeededFuture<Void>()
      return foldLeft(
        eventsList.iterator(), initialFuture
      ) { currentFuture: Future<Void>, eventRecord: EventRecord ->
        currentFuture.compose {
          log.debug("Successfully projected event #{}", eventRecord.eventMetadata.eventId)
          eventSequence.set(eventRecord.eventMetadata.eventSequence)
          action(eventRecord)
        }
      }.map { eventSequence.get() }
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

  private data class Config(
    val projectionId: String,
    val targetEndpoint: String,
    val interval: Long,
    val maxNumberOfRows: Int,
    val maxInterval: Long,
    val statsInterval: Long,
    val publisherType: String
  ) {
    companion object {
      fun create(config: JsonObject): Config {
        val projectionId = config.getString("projectionId")
        val targetEndpoint = config.getString("targetEndpoint")
        val interval = config.getLong("interval", 500)
        val maxNumberOfRows = config.getInteger("maxNumberOfRows", 500)
        val maxInterval = config.getLong("maxInterval", 60_000)
        val statsInterval = config.getLong("statsInterval", 30_000)
        val publisherType = config.getString("publisherType", "request-reply")
        return Config(projectionId, targetEndpoint, interval, maxNumberOfRows, maxInterval, statsInterval, publisherType)
      }
    }
  }
}
