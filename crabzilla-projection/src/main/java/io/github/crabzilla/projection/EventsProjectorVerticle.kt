package io.github.crabzilla.projection

import io.github.crabzilla.projection.internal.EventsScanner
import io.github.crabzilla.projection.internal.QuerySpecification
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class EventsProjectorVerticle : PostgresAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorVerticle::class.java)
    private val node: String = ManagementFactory.getRuntimeMXBean().name
    // TODO add endpoint for pause, resume, restart from N, etc (using event sourcing!)
  }

  private var greedy: Boolean = false
  private val failures = AtomicLong(0L)
  private var currentOffset = 0L

  private lateinit var options: Config
  private lateinit var scanner: EventsScanner
  private lateinit var eventsProjector: EventsProjector

  override fun start(startPromise: Promise<Void>) {
    val jsonConfig = config()
    log.info("Node {} starting with {}", node, jsonConfig.encodePrettily())
    options = Config.create(jsonConfig)
    vertx.eventBus()
      .consumer<String>("crabzilla.projector.${options.projectionName}.ping") { msg ->
        log.info(
          "Node [${msg.body()}] just asked to start this verticle. I will answer that I'm already " +
            "working from this node [$node]"
        )
        msg.reply(node)
      }
    val query = QuerySpecification.query(options.stateTypes, options.eventTypes)
    log.debug(
      "Will start projection [{}] using query [{}] in [{}] milliseconds", options.projectionName, query,
      options.initialInterval
    )
    scanner = EventsScanner(sqlClient, options.projectionName, query)
    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    eventsProjector = provider.create()
    vertx.eventBus().consumer<Void>("crabzilla.projector.${options.projectionName}") { msg ->
      action()
        .onComplete { msg.reply(null) }
    }
    // Schedule the first execution
    vertx.setTimer(options.initialInterval, handler())
    // Schedule the metrics
    vertx.setPeriodic(options.metricsInterval) {
      log.info("Projection [{}] current offset [{}]", options.projectionName, currentOffset)
    }
    subscriber
      .channel("crabzilla.events")
      .subscribeHandler {
        this.greedy = true
      }
    subscriber.connect().onSuccess {
      scanner.getCurrentOffset()
        .onSuccess { offset ->
          currentOffset = offset
          startPromise.complete()
          log.info("Projection [{}] current offset [{}]", options.projectionName, currentOffset)
          log.info("Projection [{}] started pooling events with {}", options.projectionName, options)
        }.onFailure {
          log.error(it.message)
          startPromise.fail(it)
        }
    }.onFailure {
      log.error(it.message)
      startPromise.fail(it)
    }
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

  private fun action(): Future<Int> {
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
          0 -> {
            log.debug("No new events")
            registerFailure()
          }
          else -> {
            registerSuccessOrStillBusy()
          }
        }
        succeededFuture(retrievedRows)
      }
  }

  private fun scanAndSubmit(numberOfRows: Int): Future<Int> {
    fun projectEvents(conn: SqlConnection, eventsList: List<EventRecord>): Future<Long> {
      val initialFuture = succeededFuture<Long>()
      return eventsList.fold(
        initialFuture
      ) { currentFuture: Future<Long>, eventRecord: EventRecord ->
        currentFuture.compose {
          log.debug("Will project event #{}", eventRecord.eventMetadata.eventSequence)
          val event = jsonSerDer.eventFromJson(eventRecord.eventAsjJson.toString())
          eventsProjector.project(conn, event, eventRecord.eventMetadata)
            .transform {
              if (it.failed()) failedFuture(it.cause()) else succeededFuture(eventRecord.eventMetadata.eventSequence)
            }
        }
      }
    }

    return scanner.scanPendingEvents(numberOfRows)
      .compose { eventsList ->
        if (eventsList.isEmpty()) {
          succeededFuture(0)
        } else {
          log.debug(
            "Found {} new events. The first is {} and last is {}",
            eventsList.size,
            eventsList.first().eventMetadata.eventSequence,
            eventsList.last().eventMetadata.eventSequence
          )
          pgPool.withTransaction { conn ->
            projectEvents(conn, eventsList)
              .compose { offset ->
                conn
                  .preparedQuery("update projections set sequence = $2 where name = $1")
                  .execute(Tuple.of(options.projectionName, offset))
                  .map { offset }
              }.onSuccess {
                currentOffset = it
              }.map {
                numberOfRows
              }
          }.onFailure {
            log.error(it.message)
          }
        }
      }
  }

  private data class Config(
    val projectionName: String,
    val initialInterval: Long,
    val interval: Long,
    val maxInterval: Long,
    val metricsInterval: Long,
    val maxNumberOfRows: Int,
    val stateTypes: List<String>,
    val eventTypes: List<String>
  ) {
    companion object {
      fun create(config: JsonObject): Config {
        val projectionName = config.getString("projectionName")
        val initialInterval = config.getLong("initialInterval", 10_000)
        val interval = config.getLong("interval", 100)
        val maxInterval = config.getLong("maxInterval", 30_000)
        val metricsInterval = config.getLong("metricsInterval", 30_000)
        val maxNumberOfRows = config.getInteger("maxNumberOfRows", 250)
        val stateTypesArray = config.getJsonArray("stateTypes") ?: JsonArray()
        val stateTypes = stateTypesArray.iterator().asSequence().map { it.toString() }.toList()
        val eventTypesArray = config.getJsonArray("eventTypes") ?: JsonArray()
        val eventTypes = eventTypesArray.iterator().asSequence().map { it.toString() }.toList()

        return Config(
          projectionName = projectionName,
          initialInterval = initialInterval,
          interval = interval,
          maxInterval = maxInterval,
          metricsInterval = metricsInterval,
          maxNumberOfRows = maxNumberOfRows,
          stateTypes = stateTypes,
          eventTypes = eventTypes
        )
      }
    }
  }
}
