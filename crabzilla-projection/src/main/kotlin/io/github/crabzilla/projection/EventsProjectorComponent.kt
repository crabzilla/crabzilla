package io.github.crabzilla.projection

import io.github.crabzilla.projection.EventbusTopicStrategy.GLOBAL
import io.github.crabzilla.projection.EventbusTopicStrategy.STATE_TYPE
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_PUBLISH
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_REQUEST_REPLY
import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.github.crabzilla.projection.internal.EventsScanner
import io.github.crabzilla.projection.internal.QuerySpecification
import io.github.crabzilla.stack.CrabzillaConstants
import io.github.crabzilla.stack.CrabzillaConstants.stateTypeTopic
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.projection.PgEventProjector
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class EventsProjectorComponent(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val subscriber: PgSubscriber,
  private val options: ProjectorConfig,
  private val pgEventProjector: PgEventProjector?
) {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorComponent::class.java)
    private val node: String = ManagementFactory.getRuntimeMXBean().name
    private const val GREED_INTERVAL = 100L
  }

  private var greedy = false
  private val failures = AtomicLong(0L)
  private var currentOffset = 0L
  private var isPaused = false

  private lateinit var scanner: EventsScanner
  private lateinit var projectorEndpoints: ProjectorEndpoints

  fun start(): Future<Void> {
    fun status(): JsonObject {
      return JsonObject().put("node", node).put("paused", isPaused)
        .put("greedy", greedy).put("failures", failures.get()).put("currentOffset", currentOffset)
    }

    log.info("Node {} starting with {}", node, options)

    projectorEndpoints = ProjectorEndpoints(options.projectionName)

    subscriber.connect().onSuccess {
      subscriber.channel(CrabzillaConstants.POSTGRES_NOTIFICATION_CHANNEL)
        .handler { stateType ->
          if (!greedy && (options.stateTypes.isEmpty() || options.stateTypes.contains(stateType))) {
            greedy = true
            log.debug("Greedy {}", stateType)
          }
        }
    }

    vertx.eventBus()
      .consumer<String>(projectorEndpoints.status()) { msg ->
        log.debug("Status: {}", status().encodePrettily())
        msg.reply(status())
      }

    vertx.eventBus()
      .consumer<String>(projectorEndpoints.pause()) { msg ->
        isPaused = true
        msg.reply(status())
      }

    vertx.eventBus()
      .consumer<String>(projectorEndpoints.resume()) { msg ->
        isPaused = false
        msg.reply(status())
      }

    val query = QuerySpecification.query(options.stateTypes, options.eventTypes)
    log.info(
      "Will start projection [{}] using query [{}] in [{}] milliseconds", options.projectionName, query,
      options.initialInterval
    )
    scanner = EventsScanner(pgPool, options.projectionName, query)

    val startPromise = Promise.promise<Void>()

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
        vertx.eventBus().consumer<Void>(projectorEndpoints.work()).handler { msg ->
          action()
            .onFailure {
              log.error("After action", it)
            }
            .onComplete {
              msg.reply(status())
            }
        }
        startPromise.complete()
      }.onFailure {
        startPromise.fail(it)
      }

    return startPromise.future()
  }

  fun stop() {
    log.info("Projection [{}] stopped at offset [{}]", options.projectionName, currentOffset)
  }

  private fun handler(): Handler<Long?> {
    return Handler<Long?> {
      action().onFailure { log.error("After action 2", it) }
    }
  }

  private fun action(): Future<Int> {
    fun registerFailure() {
      val nextInterval = if (greedy) GREED_INTERVAL else
        min(options.maxInterval, options.interval * failures.incrementAndGet())
      vertx.setTimer(nextInterval, handler())
      log.debug("Rescheduled to next {} milliseconds", nextInterval)
    }

    fun registerSuccessOrStillBusy() {
      failures.set(0)
      val nextInterval = if (greedy) GREED_INTERVAL else options.interval
      vertx.setTimer(nextInterval, handler())
      log.debug("Rescheduled to next {} milliseconds", nextInterval)
    }

    fun justReschedule() {
      vertx.setTimer(options.interval, handler())
      log.debug("Rescheduled to next {} milliseconds", options.interval)
    }
    if (isPaused) {
      justReschedule()
      return succeededFuture(0)
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
          val succeeded = succeededFuture(eventRecord.metadata.eventSequence)
          val topic = when (options.eventbusTopicStrategy) {
            GLOBAL -> CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC
            STATE_TYPE -> stateTypeTopic(eventRecord.metadata.stateType)
          }
          when (options.projectorStrategy) {
            POSTGRES_SAME_TRANSACTION -> {
              log.debug("Will project event {} to postgres", eventRecord.metadata.eventSequence)
              pgEventProjector!!.project(conn, eventRecord)
                .transform {
                  if (it.failed()) failedFuture(it.cause()) else succeeded
                }
            }
            EVENTBUS_REQUEST_REPLY -> {
              log.debug("Will request/reply event {} to topic {}", eventRecord.metadata.eventSequence, topic)
              vertx.eventBus().request<Void>(topic, eventRecord.toJsonObject())
                .compose {
                  succeeded
                }
            }
            EVENTBUS_PUBLISH -> {
              log.debug("Will notify event {} to topic {}", eventRecord.metadata.eventSequence, topic)
              vertx.eventBus().publish(topic, eventRecord.toJsonObject())
              succeeded
            }
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
            eventsList.first().metadata.eventSequence,
            eventsList.last().metadata.eventSequence
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
            log.error("After action 3", it)
          }
        }
      }
  }
}
