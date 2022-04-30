package io.github.crabzilla.projection

import io.github.crabzilla.CrabzillaConstants
import io.github.crabzilla.CrabzillaConstants.EVENTBUS_GLOBAL_TOPIC
import io.github.crabzilla.EventProjector
import io.github.crabzilla.EventRecord
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_PUBLISH
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_REQUEST_REPLY
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_REQUEST_REPLY_BLOCKING
import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.github.crabzilla.projection.internal.EventsScanner
import io.github.crabzilla.projection.internal.QuerySpecification
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
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
  private val eventProjector: EventProjector?
) {

  companion object {
    private val log = LoggerFactory.getLogger(EventsProjectorComponent::class.java)
    private val node: String = ManagementFactory.getRuntimeMXBean().name
    private const val GREED_INTERVAL = 100L
  }

  private var greedy = AtomicBoolean(false)
  private val failures = AtomicLong(0L)
  private val backOff = AtomicLong(0L)
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
        .put("backOff", backOff.get())
        .put("currentOffset", currentOffset.get())
    }
    fun startManagementEndpoints() {
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
    }
    fun startPgNotificationSubscriber(): Future<Void> {
      return subscriber.connect().onSuccess {
        subscriber.channel(CrabzillaConstants.POSTGRES_NOTIFICATION_CHANNEL)
          .handler { stateType ->
            if (!greedy.get() && (options.stateTypes.isEmpty() || options.stateTypes.contains(stateType))) {
              greedy.set(true)
              log.debug("Greedy {}", stateType)
            }
          }
      }
    }
    fun startEventsScanner() {
      val query = QuerySpecification.query(options.stateTypes, options.eventTypes)
      log.info(
        "Will start projection [{}] using query [{}] in [{}] milliseconds", options.projectionName, query,
        options.initialInterval
      )
      scanner = EventsScanner(pgPool, options.projectionName, query)
    }
    fun startProjection(): Future<Void> {
      return scanner.getCurrentOffset()
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
            action().onComplete { msg.reply(status()) }
          }
        }.mapEmpty()
    }

    log.info("Node {} starting with {}", node, options)
    projectorEndpoints = ProjectorEndpoints(options.projectionName)
    startManagementEndpoints()
    startEventsScanner()

    return startPgNotificationSubscriber()
      .compose { startProjection() }
  }

  // TODO this really does not stop...
  fun stop() {
    log.info("Projection [{}] stopped at offset [{}]", options.projectionName, currentOffset.get())
  }
  // TODO status, work, pause and resume methods


  private fun handler(): Handler<Long?> {
    return Handler<Long?> {
      action()
        .onSuccess { log.debug("Success") }
        .onFailure { log.error("Error", it) }
    }
  }

  private fun action(): Future<Long> {
    fun scanEvents(): Future<List<EventRecord>> {
      log.debug("Scanning for new events for projection {}", options.projectionName)
      return scanner.scanPendingEvents(options.maxNumberOfRows)
    }
    fun requestEventbus(eventsList: List<EventRecord>): Future<Long> {
      val eventsAsJson: List<JsonObject> = eventsList.map { it.toJsonObject() }
      val array = JsonArray(eventsAsJson)
      log.info("Will publish ${eventsAsJson.size}  -> ${array.encodePrettily()}")
      val promise = Promise.promise<Long>()
      vertx.eventBus().request<Long>(EVENTBUS_GLOBAL_TOPIC, array) {
        if (it.failed()) {
          log.error("Failed", it.cause())
          promise.fail(it.cause())
        } else {
          log.error("Got response {}", it.result().body())
          promise.complete(eventsList.last().metadata.eventSequence)
        }
      }
      return promise.future()
    }
    fun requestEventbusBlocking(eventsList: List<EventRecord>): Future<Long> {
      val eventsAsJson: List<JsonObject> = eventsList.map { it.toJsonObject() }
      val array = JsonArray(eventsAsJson)
      log.info("Will publish ${eventsAsJson.size}  -> ${array.encodePrettily()}")
      return vertx.executeBlocking<Long> { promise ->
        vertx.eventBus().request<Long>(EVENTBUS_GLOBAL_TOPIC, array) {
          if (it.failed()) {
            log.error("Failed", it.cause())
            promise.fail(it.cause())
          } else {
            log.error("Got response {}", it.result().body())
            promise.complete(eventsList.last().metadata.eventSequence)
          }
        }
      }
    }
    fun projectEventsToPostgres(conn: SqlConnection, eventsList: List<EventRecord>): Future<Void> {
      val initialFuture = succeededFuture<Void>()
      return eventsList.fold(
        initialFuture
      ) { currentFuture: Future<Void>, eventRecord: EventRecord ->
        currentFuture.compose {
          log.debug("Will project event {} to postgres", eventRecord.metadata.eventSequence)
          eventProjector!!.project(conn, eventRecord)
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
      val nextInterval = min(options.maxInterval, options.interval * backOff.incrementAndGet())
      vertx.setTimer(nextInterval, handler())
      log.debug("registerNoNewEvents - Rescheduled to next {} milliseconds", nextInterval)
    }
    fun registerFailure() {
      greedy.set(false)
      val jitter = ((0..5).random() * 200) // this may break test
      val nextInterval = min(options.maxInterval, (options.interval * failures.incrementAndGet()) + jitter)
      vertx.setTimer(nextInterval, handler())
      log.debug("registerFailure - Rescheduled to next {} milliseconds", nextInterval)
    }
    fun registerSuccess() {
      failures.set(0)
      backOff.set(0)
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
          succeededFuture(0)
        } else {
          log.debug("Found {} new events. The first is {} and last is {}", eventsList.size,
            eventsList.first().metadata.eventSequence,
            eventsList.last().metadata.eventSequence
          )
          when (options.projectorStrategy) {
            POSTGRES_SAME_TRANSACTION -> {
              pgPool.withTransaction { conn ->
                projectEventsToPostgres(conn, eventsList)
                  .compose { updateOffset(conn, eventsList.last().metadata.eventSequence) }
              }.map { eventsList.last().metadata.eventSequence }
            }
            EVENTBUS_REQUEST_REPLY-> {
              requestEventbus(eventsList)
                .compose { eventSequence -> pgPool.withTransaction { conn ->
                  updateOffset(conn, eventSequence)
                  }
                }
                .map { eventsList.last().metadata.eventSequence }
            }
            EVENTBUS_REQUEST_REPLY_BLOCKING -> {
              requestEventbusBlocking(eventsList)
                .compose { eventSequence -> pgPool.withTransaction { conn ->
                  updateOffset(conn, eventSequence)
                  }
                }
                .map { eventsList.last().metadata.eventSequence }
            }
            EVENTBUS_PUBLISH -> {
              val eventsAsJson: List<JsonObject> = eventsList.map { it.toJsonObject() }
              val array = JsonArray(eventsAsJson)
              log.info("Will publish ${eventsAsJson.size}  -> ${array.encodePrettily()}")
              vertx.eventBus().publish(EVENTBUS_GLOBAL_TOPIC, array)
              succeededFuture(eventsList.last().metadata.eventSequence)
                .compose { eventSequence -> pgPool.withTransaction { conn ->
                  updateOffset(conn, eventSequence)
                  }
                }
                .map { eventsList.last().metadata.eventSequence }
            }
          }.onSuccess {
            currentOffset.set(it)
            registerSuccess()
          }
        }
      }.onFailure {
        registerFailure()
      }.onComplete {
        isBusy.set(false)
      }
  }
}
