package io.github.crabzilla.subscription

import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.context.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.ViewTrigger
import io.github.crabzilla.subscription.internal.QuerySpecification
import io.github.crabzilla.subscription.internal.SubscriptionDao
import io.github.crabzilla.subscription.internal.SubscriptionEndpoints
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class SubscriptionComponentImpl(
  val crabzillaContext: CrabzillaContext,
  val spec: SubscriptionSpec,
  private val config: SubscriptionConfig = SubscriptionConfig(),
  private val viewEffect: SubscriptionApiViewEffect,
  private val viewTrigger: ViewTrigger? = null,
  // TODO perhaps a FailurePolice in order to decide what to do when N failures, etc
) : SubscriptionComponent {
  companion object {
    private val node: String = ManagementFactory.getRuntimeMXBean().name
    private const val GREED_INTERVAL = 100L
  }

  private val log =
    LoggerFactory
      .getLogger("${SubscriptionComponentImpl::class.java.simpleName}-${spec.subscriptionName}")
  private val greedy = AtomicBoolean(false)
  private val failures = AtomicLong(0L)
  private val backOff = AtomicLong(0L)
  private var currentOffset = 0L
  private val isPaused = AtomicBoolean(false)
  private val isBusy = AtomicBoolean(false)

  private lateinit var subscriptionEndpoints: SubscriptionEndpoints
  private val query = QuerySpecification.queryFor(spec.stateTypes, spec.eventTypes)

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
      val eventBus = crabzillaContext.vertx.eventBus()
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
      val subscriber = crabzillaContext.newPgSubscriber()
      subscriber.connect()
        .onSuccess {
          subscriber.channel(POSTGRES_NOTIFICATION_CHANNEL)
            .handler { stateType ->
              if (!greedy.get() && (spec.stateTypes.isEmpty() || spec.stateTypes.contains(stateType))) {
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

    fun startProjection(): Future<Void> {
      log.debug(
        "Will start subscription [{}] using query [{}] in [{}] milliseconds",
        spec.subscriptionName,
        query,
        config.initialInterval,
      )
      return crabzillaContext.pgPool.withTransaction { sqlConnection ->
        val subscriptionDao = SubscriptionDao(sqlConnection, spec.subscriptionName, query)
        subscriptionDao.lockSubscription()
          .compose {
            subscriptionDao.getOffsets()
              .onSuccess { offsets ->
                currentOffset = offsets.subscriptionOffset
                greedy.set(offsets.globalOffset > currentOffset)
              }
              .onSuccess { offsets ->
                val effectiveInitialInterval = if (greedy.get()) 100 else config.initialInterval
                log.info(
                  "Subscription [{}] current offset [{}] global offset [{}]",
                  spec.subscriptionName,
                  currentOffset,
                  offsets.globalOffset,
                )
                log.info("Subscription [{}] started pooling events with {}", spec.subscriptionName, config)
                // Schedule the first execution
                crabzillaContext.vertx.setTimer(effectiveInitialInterval, handler())
                // Schedule the metrics
                crabzillaContext.vertx.setPeriodic(config.metricsInterval) {
                  log.info("Subscription [{}] current offset [{}]", spec.subscriptionName, currentOffset)
                }
              }.mapEmpty()
          }
      }
    }

    log.info("Node {} starting with {}", node, config)
    subscriptionEndpoints = SubscriptionEndpoints(spec.subscriptionName)
    startManagementEndpoints()

    return startPgNotificationSubscriber()
      .compose { startProjection() }
  }

  private fun handler(): Handler<Long> {
    return Handler<Long> {
      action()
    }
  }

  private fun action(): Future<Void> {
    fun registerNoNewEvents() {
      greedy.set(false)
      val jitter = config.jitterFunction.invoke()
      val nextInterval = min(config.maxInterval, config.interval * backOff.incrementAndGet() + jitter)
      crabzillaContext.vertx.setTimer(nextInterval, handler())
      log.debug("registerNoNewEvents - Rescheduled to next {} milliseconds", nextInterval)
    }

    fun registerFailure(throwable: Throwable) {
      greedy.set(false)
      val jitter = config.jitterFunction.invoke()
      val nextInterval = min(config.maxInterval, (config.interval * failures.incrementAndGet()) + jitter)
      crabzillaContext.vertx.setTimer(nextInterval, handler())
      log.error("registerFailure - Rescheduled to next {} milliseconds", nextInterval, throwable)
    }

    fun registerSuccess(eventSequence: Long) {
      currentOffset = eventSequence
      failures.set(0)
      backOff.set(0)
      val nextInterval = if (greedy.get()) GREED_INTERVAL else config.interval
      crabzillaContext.vertx.setTimer(nextInterval, handler())
      log.debug("registerSuccess - Rescheduled to next {} milliseconds", nextInterval)
    }

    fun justReschedule() {
      crabzillaContext.vertx.setTimer(config.interval, handler())
      log.debug("It is busy=$isBusy or paused=$isPaused. Will just reschedule.")
      log.debug("justReschedule - Rescheduled to next {} milliseconds", config.interval)
    }

    fun publish(
      subscriptionDao: SubscriptionDao,
      eventsList: List<EventRecord>,
    ): Future<Void> {
      log.debug(
        "Found {} new events. The first is {} and last is {}",
        eventsList.size,
        eventsList.first().metadata.eventSequence,
        eventsList.last().metadata.eventSequence,
      )

      return EventRecordProjector(subscriptionDao.sqlConnection, viewEffect, viewTrigger)
        .handle(eventsList)
        .compose { subscriptionDao.updateOffset(spec.subscriptionName, eventsList.last().metadata.eventSequence) }
    }

    if (isBusy.get() || isPaused.get()) {
      justReschedule()
      return succeededFuture()
    }

    isBusy.set(true)

    return crabzillaContext.pgPool.withTransaction { sqlConnection ->
      val subscriptionDao = SubscriptionDao(sqlConnection, spec.subscriptionName, query)
      subscriptionDao.lockSubscription()
        .compose {
          log.debug("Scanning for new events for subscription {}", spec.subscriptionName)
          subscriptionDao.scanPendingEvents(1000)
            .compose { eventsList ->
              if (eventsList.isEmpty()) {
                registerNoNewEvents()
                succeededFuture()
              } else {
                publish(subscriptionDao, eventsList)
                  .onSuccess {
                    registerSuccess(eventsList.last().metadata.eventSequence)
                  }
              }
            }
        }.onFailure {
          registerFailure(it)
        }.onComplete {
          isBusy.set(false)
        }
    }
  }

  override fun extractApi(): SubscriptionApi {
    return SubscriptionFactoryImpl().create(this)
  }
}
