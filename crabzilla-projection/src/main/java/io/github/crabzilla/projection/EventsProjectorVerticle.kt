package io.github.crabzilla.projection

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class EventsProjectorVerticle : PostgresAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorVerticle::class.java)
    // TODO add endpoint for pause, resume, restart from N, etc (using event sourcing!)
  }

  private val failures = AtomicLong(0L)

  private lateinit var options: Config
  private lateinit var scanner: EventsScanner
  private lateinit var eventsProjector: EventsProjector

  override fun start() {
    options = Config.create(config())
    log.info("Starting with {}", options)
    scanner = EventsScanner(sqlClient, options.projectionName)
    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    eventsProjector = provider.create()
    vertx.eventBus().consumer<Void>("crabzilla.projector.${options.projectionName}") { msg ->
      action()
        .onComplete { msg.reply(null) }
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
    log.debug("Scanning for new events for projection {}", options.projectionName)
    return scanAndSubmit(options.maxNumberOfRows)
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

  private fun scanAndSubmit(numberOfRows: Int): Future<Long> {
    fun projectEvents(eventsList: List<EventRecord>): Future<Long> {
      fun projectEvent(eventRecord: EventRecord): Future<Void> {
        val eventSequence = eventRecord.eventMetadata.eventSequence
        val event = jsonSerDer.eventFromJson(eventRecord.eventAsjJson.toString())
        return pgPool.withTransaction { conn ->
          eventsProjector.project(conn, event, eventRecord.eventMetadata)
            .compose {
              conn
                .preparedQuery("update projections set sequence = $2 where name = $1 and sequence < $2")
                .execute(Tuple.of(options.projectionName, eventSequence))
            }
        }.mapEmpty()
      }

      val initialFuture = Future.succeededFuture<Long>()
      return eventsList.fold(
        initialFuture
      ) { currentFuture: Future<Long>, eventRecord: EventRecord ->
        currentFuture.compose {
          log.debug("Will project event #{}", eventRecord.eventMetadata.eventId)
          projectEvent(eventRecord)
            .map { eventRecord.eventMetadata.eventSequence }
        }
      }
    }

    return scanner.scanPendingEvents(numberOfRows)
      .compose { eventsList ->
        if (eventsList.isEmpty()) {
          Future.succeededFuture(0)
        } else {
          log.debug("Found {} new events. The last one is {}", eventsList.size, eventsList.last().eventMetadata.eventId)
          projectEvents(eventsList)
        }
      }
  }

  private data class Config(
    val projectionName: String,
    val initialInterval: Long,
    val interval: Long,
    val maxInterval: Long,
    val metricsInterval: Long,
    val maxNumberOfRows: Int
  ) {
    companion object {
      fun create(config: JsonObject): Config {
        val projectionName = config.getString("projectionName")
        val initialInterval = config.getLong("initialInterval", 10_000)
        val interval = config.getLong("interval", 500)
        val maxInterval = config.getLong("maxInterval", 30_000)
        val metricsInterval = config.getLong("metricsInterval", 10_000)
        val maxNumberOfRows = config.getInteger("maxNumberOfRows", 500)
        return Config(
          projectionName = projectionName,
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
