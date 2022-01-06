package io.github.crabzilla.pgclient.projection

import io.github.crabzilla.core.EventTopics
import io.github.crabzilla.pgclient.EventsProjector
import io.github.crabzilla.pgclient.PgClientAbstractVerticle
import io.github.crabzilla.pgclient.projection.internal.EventsScanner
import io.github.crabzilla.pgclient.projection.internal.QuerySpecification
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

class EventsProjectorVerticle : PgClientAbstractVerticle() {

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
    subscriber.connect().onSuccess {
      subscriber.channel(EventTopics.STATE_TOPIC.name.lowercase())
        .handler { stateType ->
          if (!greedy && (options.stateTypes.isEmpty() || options.stateTypes.contains(stateType))) {
            greedy = true
            log.debug("Greedy {}", stateType)
          }
        }
    }
    vertx.eventBus()
      .consumer<String>("crabzilla.projectors.${options.projectionName}.ping") { msg ->
        log.info(
          "Node [${msg.body()}] just asked to start this verticle. I will answer that I'm already " +
            "working from this node [$node]"
        )
        msg.reply(node)
      }
    val query = QuerySpecification.query(options.stateTypes, options.eventTypes)
    log.info(
      "Will start projection [{}] using query [{}] in [{}] milliseconds", options.projectionName, query,
      options.initialInterval
    )
    scanner = EventsScanner(pgPool, options.projectionName, query)
    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    eventsProjector = provider.create()

    scanner.getCurrentOffset()
      .onSuccess { offset ->
        currentOffset = offset
      }
      .compose {
        scanner.getGlobalOffset()
      }
      .onSuccess { globalOffset ->
        greedy = globalOffset > currentOffset
        val effectiveInitialInterval = if (greedy) 1 else options.initialInterval
        log.info(
          "Projection [{}] current offset [{}] global offset [{}] will start in [{}] ms",
          options.projectionName, currentOffset, globalOffset, effectiveInitialInterval
        )
        log.info("Projection [{}] started pooling events with {}", options.projectionName, options)
        // Schedule the first execution
        vertx.setTimer(effectiveInitialInterval, handler())
        // Schedule the metrics
        vertx.setPeriodic(options.metricsInterval) {
          log.info("Projection [{}] current offset [{}]", options.projectionName, currentOffset)
        }
        vertx.eventBus().consumer<Void>("crabzilla.projectors.${options.projectionName}") { msg ->
          action()
            .onComplete { msg.reply(null) }
        }
        startPromise.complete()
      }.onFailure {
        startPromise.fail(it)
      }
  }

  override fun stop() {
    log.info("Projection [{}] stopped at offset [{}]", options.projectionName, currentOffset)
  }

  private fun handler(): Handler<Long?> {
    return Handler<Long?> {
      action()
        .onFailure { log.error(it.message, it) }
    }
  }

  private fun action(): Future<Int> {
    fun registerFailure() {
      val nextInterval = if (greedy) 1 else min(options.maxInterval, options.interval * failures.incrementAndGet())
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
            greedy = false
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
          eventsProjector.project(conn, eventRecord.eventAsjJson, eventRecord.eventMetadata)
            .transform {
              if (it.failed()) failedFuture(it.cause()) else succeededFuture(eventRecord.eventMetadata.eventSequence)
            }.eventually {
              pgPool
                .preparedQuery("NOTIFY " + EventTopics.VIEW_TOPIC.name.lowercase() + ", '${options.viewName}'")
                .execute()
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
    val viewName: String,
    val initialInterval: Long,
    val interval: Long,
    val maxNumberOfRows: Int,
    val maxInterval: Long,
    val metricsInterval: Long,
    val stateTypes: List<String>,
    val eventTypes: List<String>,
  ) {
    companion object {
      fun create(config: JsonObject): Config {
        val projectionName = config.getString("projectionName")
        val viewName = config.getString("viewName")
        val initialInterval = config.getLong("initialInterval", 5_000)
        val interval = config.getLong("interval", 250)
        val maxNumberOfRows = config.getInteger("maxNumberOfRows", 250)
        val maxInterval = config.getLong("maxInterval", 60_000)
        val metricsInterval = config.getLong("metricsInterval", 60_000)
        val stateTypesArray = config.getJsonArray("stateTypes") ?: JsonArray()
        val stateTypes = stateTypesArray.iterator().asSequence().map { it.toString() }.toList()
        val eventTypesArray = config.getJsonArray("eventTypes") ?: JsonArray()
        val eventTypes = eventTypesArray.iterator().asSequence().map { it.toString() }.toList()

        return Config(
          projectionName = projectionName,
          viewName = viewName,
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
