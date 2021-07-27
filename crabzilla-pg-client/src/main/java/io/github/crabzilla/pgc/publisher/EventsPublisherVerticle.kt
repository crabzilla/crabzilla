package io.github.crabzilla.pgc.publisher

import io.github.crabzilla.pgc.PgcAbstractVerticle
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class EventsPublisherVerticle : PgcAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsPublisherVerticle::class.java)

    // TODO this must be by instance: add endpoint for pause, resume, restart from N, etc (using event sourcing!)
  }

  private val action: Handler<Long?> = handler()
  private val failures = AtomicLong(0L)
  private val lastEventPublishedRef = AtomicLong(0L)

  private lateinit var options: Config
  lateinit var scanner: EventsScanner

  override fun start() {

    options = Config.create(config())

    val sqlClient = sqlClient(config())
    scanner = EventsScanner(sqlClient, options.publicationId)
    options = Config.create(config())

    // Schedule the first execution
    vertx.setTimer(options.initialInterval, action)

    vertx.setPeriodic(options.metricsInterval) {
      publishMetrics()
    }

    log.info("Started pooling events with {}", options)
    publishMetrics()
  }

  override fun stop() {
    log.info("Stopped")
  }

  fun publishMetrics() {
    val metric = JsonObject() // TODO also publish errors
      .put("publicationId", options.publicationId)
      .put("sequence", lastEventPublishedRef.get())
    vertx.eventBus().publish("crabzilla.publications", metric)
  }

  private fun handler(): Handler<Long?> {
    return Handler {
      action()
    }
  }

  fun action(): Future<Long> {
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
      fun action(eventRecord: EventRecord): Future<Void> {
        return vertx.eventBus().request<Void>(options.targetEndpoint, eventRecord.toJsonObject()).mapEmpty()
      }
      val eventSequence = AtomicLong(0)
      val initialFuture = Future.succeededFuture<Void>()
      return eventsList.fold(
        initialFuture
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
                  lastEventPublishedRef.set(lastEventPublished)
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
    val publicationId: String,
    val targetEndpoint: String,
    val initialInterval: Long,
    val interval: Long,
    val maxInterval: Long,
    val metricsInterval: Long,
    val maxNumberOfRows: Int
  ) {
    companion object {
      fun create(config: JsonObject): Config {
        val publicationId = config.getString("publicationId")
        val targetEndpoint = config.getString("targetEndpoint")
        val initialInterval = config.getLong("initialInterval", 10_000)
        val interval = config.getLong("interval", 500)
        val maxInterval = config.getLong("maxInterval", 30_000)
        val metricsInterval = config.getLong("metricsInterval", 10_000)
        val maxNumberOfRows = config.getInteger("maxNumberOfRows", 500)
        return Config(
          publicationId = publicationId,
          targetEndpoint = targetEndpoint,
          initialInterval = initialInterval,
          interval = interval,
          maxInterval = maxInterval,
          metricsInterval = metricsInterval,
          maxNumberOfRows = maxNumberOfRows
        )
      }
    }
  }
}
