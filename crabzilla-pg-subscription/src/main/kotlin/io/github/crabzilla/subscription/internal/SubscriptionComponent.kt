package io.github.crabzilla.subscription.internal

import CrabzillaContext
import CrabzillaContext.Companion.EVENTBUS_GLOBAL_TOPIC
import CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import EventProjector
import EventRecord
import io.github.crabzilla.subscription.SubscriptionConfig
import io.github.crabzilla.subscription.SubscriptionSink.*
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

internal class SubscriptionComponent( // TODO refactor
  private val crabzillaContext: CrabzillaContext,
  private val options: SubscriptionConfig,
  private val eventProjector: EventProjector?,
) {

  companion object {
    private val node: String = ManagementFactory.getRuntimeMXBean().name
    private const val GREED_INTERVAL = 100L
  }

  private val log = LoggerFactory
    .getLogger("${SubscriptionComponent::class.java.simpleName}-${options.subscriptionName}")
  private var greedy = AtomicBoolean(false)
  private val failures = AtomicLong(0L)
  private val backOff = AtomicLong(0L)
  private var currentOffset = 0L
  private var isPaused = AtomicBoolean(false)
  private var isBusy = AtomicBoolean(false)
  // TODO add isLenient (ignore failed projections) to status, SubscriptionEndpoints and projection

  private lateinit var scanner: EventsScanner
  private lateinit var subscriptionEndpoints: SubscriptionEndpoints

  fun start(): Future<Void> {
    fun currentStatus(): JsonObject {
      return JsonObject()
        .put("node", node)
        .put("paused", isPaused.get())
        .put("busy", isBusy.get())
        .put("greedy", greedy.get())
        .put("failures", failures.get())
        .put("backOff", backOff.get())
        .put("currentOffset", currentOffset)
    }

    fun startManagementEndpoints() {
      val eventBus = crabzillaContext.vertx().eventBus()
      with(subscriptionEndpoints) {
        eventBus
          .consumer<Nothing>(status()) { msg ->
            log.debug("Status: {}", currentStatus().encodePrettily())
            msg.reply(currentStatus())
          }
        eventBus
          .consumer<Nothing>(pause()) { msg ->
            log.debug("Status: {}", currentStatus().encodePrettily())
            isPaused.set(true)
            msg.reply(currentStatus())
          }
        eventBus
          .consumer<Nothing>(resume()) { msg ->
            log.debug("Status: {}", currentStatus().encodePrettily())
            isPaused.set(false)
            msg.reply(currentStatus())
          }
        eventBus.consumer<Nothing>(handle()) { msg ->
          log.debug("Will handle")
          action()
            .onFailure { log.error(it.message) }
            .onSuccess { log.debug("Handle finished") }
            .onComplete {
              msg.reply(currentStatus())
            }
        }
      }
    }

    fun startPgNotificationSubscriber(): Future<Void> {
      val promise = Promise.promise<Void>()
      val subscriber = crabzillaContext.pgSubscriber()
      subscriber.connect()
        .onSuccess {
          subscriber.channel(POSTGRES_NOTIFICATION_CHANNEL)
            .handler { stateType ->
              if (!greedy.get() && (options.stateTypes.isEmpty() || options.stateTypes.contains(stateType))) {
                greedy.set(true)
                log.debug("Greedy {}", stateType)
              }
            }
          promise.complete()
        }.onFailure {
          promise.fail(it)
        }
      return promise.future()
    }

    fun startEventsScanner() {
      val query = QuerySpecification.query(options.stateTypes, options.eventTypes)
      log.debug(
        "Will start subscription [{}] using query [{}] in [{}] milliseconds", options.subscriptionName, query,
        options.initialInterval
      )
      scanner = EventsScanner(crabzillaContext.pgPool(), options.subscriptionName, query)
    }

    fun startProjection(): Future<Void> {
      return scanner.getCurrentOffset()
        .onSuccess { offset ->
          currentOffset = offset
        }
        .compose {
          scanner.getGlobalOffset()
        }
        .onSuccess { globalOffset ->
          greedy.set(globalOffset > currentOffset)
          val effectiveInitialInterval = if (greedy.get()) 1 else options.initialInterval
          log.info(
            "Subscription [{}] current offset [{}] global offset [{}]",
            options.subscriptionName, currentOffset, globalOffset
          )
          log.info("Subscription [{}] started pooling events with {}", options.subscriptionName, options)
          // Schedule the first execution
          crabzillaContext.vertx().setTimer(effectiveInitialInterval, handler())
          // Schedule the metrics
          crabzillaContext.vertx().setPeriodic(options.metricsInterval) {
            log.info("Subscription [{}] current offset [{}]", options.subscriptionName, currentOffset)
          }
        }.mapEmpty()
    }

    log.info("Node {} starting with {}", node, options)
    subscriptionEndpoints = SubscriptionEndpoints(options.subscriptionName)
    startManagementEndpoints()
    startEventsScanner()

    return startPgNotificationSubscriber()
      .compose { startProjection() }
  }

  private fun handler(): Handler<Long> {
    return Handler<Long> {
      action()
    }
  }

  private fun action(): Future<Void> {
    fun scanEvents(): Future<List<EventRecord>> {
      log.debug("Scanning for new events for subscription {}", options.subscriptionName)
      return scanner.scanPendingEvents(options.maxNumberOfRows)
    }

    fun requestEventbus(eventsList: List<EventRecord>): Future<Long> {
      val eventsAsJson: List<JsonObject> = eventsList.map { it.toJsonObject() }
      val array = JsonArray(eventsAsJson)
      log.debug("Will publish {} events", eventsAsJson.size)
      val promise = Promise.promise<Long>()
      crabzillaContext.vertx().eventBus().request<Long>(EVENTBUS_GLOBAL_TOPIC, array) {
        if (it.failed()) {
          promise.fail(it.cause())
        } else {
          log.debug("Got response {}", it.result().body())
          promise.complete(eventsList.last().metadata.eventSequence)
        }
      }
      return promise.future()
    }

    fun requestEventbusBlocking(eventsList: List<EventRecord>): Future<Long> {
      val eventsAsJson: List<JsonObject> = eventsList.map { it.toJsonObject() }
      val array = JsonArray(eventsAsJson)
      log.debug("Will publish ${eventsAsJson.size}  -> ${array.encodePrettily()}")
      return crabzillaContext.vertx().executeBlocking<Long> { promise ->
        crabzillaContext.vertx().eventBus().request<Long>(EVENTBUS_GLOBAL_TOPIC, array) {
          if (it.failed()) {
            promise.fail(it.cause())
          } else {
            log.debug("Got response {}", it.result().body())
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
        .preparedQuery("update subscriptions set sequence = $2 where name = $1")
        .execute(Tuple.of(options.subscriptionName, offset))
        .mapEmpty()
    }

    fun registerNoNewEvents() {
      greedy.set(false)
      val jitter = ((0..5).random() * 200)
      val nextInterval = min(options.maxInterval, options.interval * backOff.incrementAndGet() + jitter)
      crabzillaContext.vertx().setTimer(nextInterval, handler())
      log.debug("registerNoNewEvents - Rescheduled to next {} milliseconds", nextInterval)
    }

    fun registerFailure(throwable: Throwable) {
      greedy.set(false)
      val jitter = ((0..5).random() * 200)
      val nextInterval = min(options.maxInterval, (options.interval * failures.incrementAndGet()) + jitter)
      crabzillaContext.vertx().setTimer(nextInterval, handler())
      log.error("registerFailure - Rescheduled to next {} milliseconds", nextInterval, throwable)
    }

    fun registerSuccess(eventSequence: Long) {
      currentOffset = eventSequence
      failures.set(0)
      backOff.set(0)
      val nextInterval = if (greedy.get()) GREED_INTERVAL else options.interval
      crabzillaContext.vertx().setTimer(nextInterval, handler())
      log.debug("registerSuccess - Rescheduled to next {} milliseconds", nextInterval)
    }

    fun justReschedule() {
      crabzillaContext.vertx().setTimer(options.interval, handler())
      log.debug("It is busy=$isBusy or paused=$isPaused. Will just reschedule.")
      log.debug("justReschedule - Rescheduled to next {} milliseconds", options.interval)
    }

    fun publish(eventsList: List<EventRecord>): Future<Void> {
      log.debug("Found {} new events. The first is {} and last is {}", eventsList.size,
        eventsList.first().metadata.eventSequence,
        eventsList.last().metadata.eventSequence
      )
      return when (options.sink) {
        POSTGRES_PROJECTOR -> {
          crabzillaContext.pgPool().withTransaction { conn ->
            projectEventsToPostgres(conn, eventsList)
              .compose { updateOffset(conn, eventsList.last().metadata.eventSequence) }
          }
        }
        EVENTBUS_REQUEST_REPLY -> {
          requestEventbus(eventsList)
            .compose { eventSequence ->
              crabzillaContext.pgPool().withTransaction { conn ->
                updateOffset(conn, eventSequence)
              }
            }
        }
        EVENTBUS_REQUEST_REPLY_BLOCKING -> {
          requestEventbusBlocking(eventsList)
            .compose { eventSequence ->
              crabzillaContext.pgPool().withTransaction { conn ->
                updateOffset(conn, eventSequence)
              }
            }
        }
        EVENTBUS_PUBLISH -> {
          val eventsAsJson: List<JsonObject> = eventsList.map { it.toJsonObject() }
          val array = JsonArray(eventsAsJson)
          log.info("Will publish ${eventsAsJson.size}  -> ${array.encodePrettily()}")
          crabzillaContext.vertx().eventBus().publish(EVENTBUS_GLOBAL_TOPIC, array)
          succeededFuture(eventsList.last().metadata.eventSequence)
            .compose { eventSequence ->
              crabzillaContext.pgPool().withTransaction { conn ->
                updateOffset(conn, eventSequence)
              }
            }
        }
      }
    }
    if (isBusy.get() || isPaused.get()) {
      justReschedule()
      return succeededFuture()
    }
    isBusy.set(true)
    // TODO withTransaction on scan and project events sink
    // TODO break this component into one impl per sink

    return scanEvents()
      .compose { eventsList ->
        if (eventsList.isEmpty()) {
          registerNoNewEvents()
          succeededFuture()
        } else {
          publish(eventsList)
            .onSuccess {
              registerSuccess(eventsList.last().metadata.eventSequence)
            }
        }
      }.onFailure {
        registerFailure(it)
      }.onComplete {
        isBusy.set(false)
      }
  }

}