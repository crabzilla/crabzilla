package io.github.crabzilla.command.publisher

import io.github.crabzilla.command.EventRecord
import io.github.crabzilla.command.PostgresAbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class EventsPublisherVerticle : PostgresAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsPublisherVerticle::class.java)
    // TODO add endpoint for pause, resume, restart from N, etc (using event sourcing!)
  }

  private val failures = AtomicLong(0L) // TODO não precisa atomic e publisher exclusivo para projections
  private val currentOffsetRef = AtomicLong(0L)

  private lateinit var options: Config
  private lateinit var scanner: EventsScanner

  override fun start() {
    options = Config.create(config())
    log.info("Starting with {}", options)
    scanner = EventsScanner(sqlClient, options.targetEndpoint)
    vertx.eventBus().consumer<Void>("crabzilla.publisher-${options.targetEndpoint}") { msg ->
      action()
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(null) }
    }
    // Schedule the first execution
    vertx.setTimer(options.initialInterval + options.interval, handler())
    log.info("Started pooling events with {}", options)
  }

  override fun stop() {
    log.info("Stopped")
  }

  private fun handler(): Handler<Long?> {
    return Handler<Long?> {
      action()
        .onFailure { log.error(it.message, it) }
    }
  }

  private fun action(): Future<Long> {
    fun registerFailure() {
      val nextInterval = min(options.maxInterval, options.interval * failures.incrementAndGet())
      vertx.setTimer(nextInterval, handler())
      log.debug("Rescheduled to next {} milliseconds", nextInterval)
    }
    fun registerSuccessOrStillBusy() {
      failures.set(0)
      vertx.setTimer(options.interval, handler())
      log.debug("Rescheduled to next {} milliseconds", options.interval)
    }
    log.debug("Scanning new events")
    return scanAndPublish(options.maxNumberOfRows)
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

  private fun scanAndPublish(numberOfRows: Int): Future<Long> {
    fun publish(eventsList: List<EventRecord>): Future<Long> {
      val initialFuture = Future.succeededFuture<Long>()
      return eventsList.fold(
        initialFuture
      ) { currentFuture: Future<Long>, eventRecord: EventRecord ->
        currentFuture.compose {
          log.debug("Will publish event #{}", eventRecord.eventMetadata.eventId)
          vertx.eventBus().request<Void>(options.targetEndpoint, eventRecord.toJsonObject())
            .map { eventRecord.eventMetadata.eventSequence }
        }
      }
    }
    val promise = Promise.promise<Long>()
    scanner.scanPendingEvents(numberOfRows)
      .compose { eventsList ->
        if (eventsList.isEmpty()) {
          Future.failedFuture("No new events")
        } else {
          log.debug("Found {} new events. The last one is {}", eventsList.size, eventsList.last().eventMetadata.eventId)
          publish(eventsList)
        }
      }.compose { lastPublishedEvent ->
        scanner.updateOffSet(lastPublishedEvent)
          .map { lastPublishedEvent }
      }.onSuccess { lastPublishedEvent ->
        currentOffsetRef.set(lastPublishedEvent)
        promise.complete(lastPublishedEvent)
      }.onFailure {
        promise.complete(0L)
      }
      .onComplete {
        log.trace("Scan is now inactive until new request")
      }
    return promise.future()
  }

  private data class Config(
    val targetEndpoint: String,
    val initialInterval: Long,
    val interval: Long,
    val maxInterval: Long,
    val metricsInterval: Long,
    val maxNumberOfRows: Int
  ) {
    companion object {
      fun create(config: JsonObject): Config {
        val targetEndpoint = config.getString("targetEndpoint")
        val initialInterval = config.getLong("initialInterval", 10_000)
        val interval = config.getLong("interval", 500)
        val maxInterval = config.getLong("maxInterval", 30_000)
        val metricsInterval = config.getLong("metricsInterval", 10_000)
        val maxNumberOfRows = config.getInteger("maxNumberOfRows", 500)
        return Config(
          targetEndpoint = targetEndpoint,
          initialInterval = initialInterval,
          interval = interval,
          maxInterval = maxInterval,
          metricsInterval = metricsInterval, // TODO delete
          maxNumberOfRows = maxNumberOfRows
        )
      }
    }
  }
}
