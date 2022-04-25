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
import java.util.concurrent.atomic.AtomicBoolean
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

  private var greedy = AtomicBoolean(false)
  private val failures = AtomicLong(0L)
  private var currentOffset = AtomicLong(0L)
  private var isPaused = AtomicBoolean(false)
  private var isBusy = AtomicBoolean(false)

  private lateinit var scanner: EventsScanner
  private lateinit var projectorEndpoints: ProjectorEndpoints

  fun start(): Future<Void> {
    fun status(): JsonObject {
      return JsonObject()
        .put("node", node)
        .put("paused", isPaused.get())
        .put("busy", isBusy.get())
        .put("greedy", greedy.get())
        .put("failures", failures.get())
        .put("currentOffset", currentOffset.get())
    }

    log.info("Node {} starting with {}", node, options)

    projectorEndpoints = ProjectorEndpoints(options.projectionName)

    subscriber.connect().onSuccess {
      subscriber.channel(CrabzillaConstants.POSTGRES_NOTIFICATION_CHANNEL)
        .handler { stateType ->
          if (!greedy.get() && (options.stateTypes.isEmpty() || options.stateTypes.contains(stateType))) {
            greedy.set(true)
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
        isPaused.set(true)
        msg.reply(status())
      }

    vertx.eventBus()
      .consumer<String>(projectorEndpoints.resume()) { msg ->
        isPaused.set(false)
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
        currentOffset.set(offset)
      }
      .compose {
        scanner.getGlobalOffset()
      }
      .onSuccess { globalOffset ->
        greedy.set(globalOffset > currentOffset.get())
        val effectiveInitialInterval = if (greedy.get()) 1 else options.initialInterval
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
      action()
        .onSuccess { log.debug("Success") }
        .onFailure { log.error("Error", it) }
    }
  }

  private fun action(): Future<Void> {
    fun scanEvents(): Future<List<EventRecord>> {
      log.debug("Scanning for new events for projection {}", options.projectionName)
      return scanner.scanPendingEvents(options.maxNumberOfRows)
    }
    fun projectEvents(conn: SqlConnection, eventsList: List<EventRecord>): Future<Void> {
      val initialFuture = succeededFuture<Void>()
      return eventsList.fold(
        initialFuture
      ) { currentFuture: Future<Void>, eventRecord: EventRecord ->
        currentFuture.compose {
          val topic = when (options.eventbusTopicStrategy) {
            GLOBAL -> CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC
            STATE_TYPE -> stateTypeTopic(eventRecord.metadata.stateType)
          }
          val publishedOk: Future<out Any> = when (options.projectorStrategy) {
            POSTGRES_SAME_TRANSACTION -> {
              log.debug("Will project event {} to postgres", eventRecord.metadata.eventSequence)
              pgEventProjector!!.project(conn, eventRecord)
            }
            EVENTBUS_REQUEST_REPLY -> {
              log.debug("Will request/reply event {} to topic {}", eventRecord.metadata.eventSequence, topic)
              vertx.eventBus().request<Void>(topic, eventRecord.toJsonObject())
            }
            EVENTBUS_PUBLISH -> {
              log.debug("Will notify event {} to topic {}", eventRecord.metadata.eventSequence, topic)
              vertx.eventBus().publish(topic, eventRecord.toJsonObject())
              succeededFuture()
            }
          }
          publishedOk.transform {
            if (it.failed()) failedFuture(it.cause()) else succeededFuture()
          }
        }
      }
    }
    fun updateOffset(conn: SqlConnection, offset: Long): Future<Void> {
      return conn
        .preparedQuery("update projections set sequence = $2 where name = $1")
        .execute(Tuple.of(options.projectionName, offset))
        .mapEmpty()
    }
    fun registerNoNewEvents() {
      greedy.set(false)
      val nextInterval = min(options.maxInterval, options.interval * failures.incrementAndGet())
      vertx.setTimer(nextInterval, handler())
      log.debug("registerNoNewEvents - Rescheduled to next {} milliseconds", nextInterval)
    }
    fun registerFailure() {
      greedy.set(false)
      val nextInterval = min(options.maxInterval, options.interval * failures.incrementAndGet())
      vertx.setTimer(nextInterval, handler())
      log.debug("registerFailure - Rescheduled to next {} milliseconds", nextInterval)
    }
    fun registerSuccess() {
      failures.set(0)
      val nextInterval = if (greedy.get()) GREED_INTERVAL else options.interval
      vertx.setTimer(nextInterval, handler())
      log.debug("Rescheduled to next {} milliseconds", nextInterval)
    }
    fun justReschedule() {
      vertx.setTimer(options.interval, handler())
      log.info("It is busy=$isBusy or paused=$isPaused. Will just reschedule.")
      log.debug("Rescheduled to next {} milliseconds", options.interval)
    }
    if (isBusy.get() || isPaused.get()) {
      justReschedule()
      return succeededFuture()
    }
    isBusy.set(true)
    return scanEvents()
      .compose { eventsList ->
        if (eventsList.isEmpty()) {
          registerNoNewEvents()
          succeededFuture<Void>(null)
        } else {
          log.debug(
            "Found {} new events. The first is {} and last is {}",
            eventsList.size,
            eventsList.first().metadata.eventSequence,
            eventsList.last().metadata.eventSequence
          )
          pgPool.withTransaction { conn ->
            projectEvents(conn, eventsList)
              .compose { updateOffset(conn, eventsList.last().metadata.eventSequence) }
              .onSuccess {
                currentOffset.set(eventsList.last().metadata.eventSequence)
                registerSuccess()
              }
          }
        }
      }.onFailure {
        registerFailure()
      }.onComplete {
        isBusy.set(false)
      }
  }
}