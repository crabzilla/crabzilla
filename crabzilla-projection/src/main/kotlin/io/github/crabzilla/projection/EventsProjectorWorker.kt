package io.github.crabzilla.projection

import io.github.crabzilla.projection.EventbusTopicStrategy.GLOBAL
import io.github.crabzilla.projection.EventbusTopicStrategy.STATE_TYPE
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_PUBLISH
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_REQUEST_REPLY
import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.github.crabzilla.projection.internal.EventsScanner
import io.github.crabzilla.projection.internal.QuerySpecification
import io.github.crabzilla.stack.CrabzillaConstants
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

class EventsProjectorWorker(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val subscriber: PgSubscriber,
  private val options: ProjectorConfig,
  private val pgEventProjector: PgEventProjector?
) {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorWorker::class.java)
    private val node: String = ManagementFactory.getRuntimeMXBean().name
    // TODO add endpoint for restart from N
  }

  private var greedy = false
  private val failures = AtomicLong(0L)
  private var currentOffset = 0L
  private var isPaused = false

  private lateinit var scanner: EventsScanner
  private lateinit var projectorEndpoints: ProjectorEndpoints

  fun start(startPromise: Promise<Void>) {
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
              log.error(it.message, it)
            }
            .onComplete {
              msg.reply(status())
            }
        }
        startPromise.complete()
      }.onFailure {
        startPromise.fail(it)
      }
  }

  fun stop() {
    log.info("Projection [{}] stopped at offset [{}]", options.projectionName, currentOffset)
  }

  private fun handler(): Handler<Long?> {
    return Handler<Long?> {
      vertx.eventBus().request<JsonObject>(projectorEndpoints.work(), null)
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

    fun justReschedule() {
      vertx.setTimer(options.interval, handler())
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
          log.debug("Will project event #{}", eventRecord.metadata.eventSequence)
          val resultIfSuccess = succeededFuture(eventRecord.metadata.eventSequence)
          val topic = when (options.eventbusTopicStrategy) {
            GLOBAL -> CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC
            STATE_TYPE -> eventRecord.metadata.stateType
          }
          when (options.projectorStrategy) {
            POSTGRES_SAME_TRANSACTION -> {
              pgEventProjector!!.project(conn, eventRecord.payload, eventRecord.metadata)
                .transform {
                  if (it.failed()) failedFuture(it.cause()) else resultIfSuccess
                }
            }
            EVENTBUS_REQUEST_REPLY -> {
              val promise = Promise.promise<Void>()
              vertx.eventBus().request<Void>(topic, eventRecord.toJsonObject()) { result ->
                if (result.failed()) {
                  promise.fail(result.cause())
                } else {
                  promise.complete()
                }
              }
              if (promise.future().succeeded()) {
                resultIfSuccess
              } else {
                failedFuture(promise.future().cause())
              }
            }
            EVENTBUS_PUBLISH -> {
              vertx.eventBus().publish(topic, eventRecord.toJsonObject())
              resultIfSuccess
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
            log.error(it.message)
          }
        }
      }
  }
}