package io.github.crabzilla.subscription

import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.context.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.ViewTrigger
import io.github.crabzilla.subscription.internal.QuerySpecification
import io.github.crabzilla.subscription.internal.SubscriptionDao
import io.github.crabzilla.subscription.internal.SubscriptionEndpoints
import io.github.crabzilla.subscription.internal.ViewEffectHandler
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
  private val logger =
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
            if (logger.isTraceEnabled) logger.trace("Status: {}", currentStatus().encodePrettily())
            msg.reply(currentStatus())
          }
        eventBus
          .consumer<Nothing>(pause()) { msg ->
            if (logger.isTraceEnabled) logger.trace("Status: {}", currentStatus().encodePrettily())
            isPaused.set(true)
            msg.reply(currentStatus())
          }
        eventBus
          .consumer<Nothing>(resume()) { msg ->
            if (logger.isTraceEnabled) logger.trace("Status: {}", currentStatus().encodePrettily())
            isPaused.set(false)
            msg.reply(currentStatus())
          }
        eventBus.consumer<Nothing>(handle()) { msg ->
          if (logger.isTraceEnabled) logger.trace("Will handle")
          action()
            .onFailure { logger.error(it.message) }
            .onSuccess { if (logger.isTraceEnabled) logger.trace("Handle finished") }
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
                if (logger.isTraceEnabled) logger.trace("Greedy {}", stateType)
              }
            }
          promise.complete()
        }.onFailure {
          promise.fail(it)
        }
      return promise.future()
    }

    fun startProjection(): Future<Void> {
      if (logger.isTraceEnabled) {
        logger.trace(
          "Will start subscription [{}] using query [{}] in [{}] milliseconds",
          spec.subscriptionName,
          query,
          config.initialInterval,
        )
      }
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
                logger.info(
                  "Subscription [{}] current offset [{}] global offset [{}]",
                  spec.subscriptionName,
                  currentOffset,
                  offsets.globalOffset,
                )
                logger.info("Subscription [{}] started pooling events with {}", spec.subscriptionName, config)
                // Schedule the first execution
                crabzillaContext.vertx.setTimer(effectiveInitialInterval, handler())
                // Schedule the metrics
                crabzillaContext.vertx.setPeriodic(config.metricsInterval) {
                  logger.info("Subscription [{}] current offset [{}]", spec.subscriptionName, currentOffset)
                }
              }.mapEmpty()
          }
      }
    }

    logger.info("Node {} starting with {}", node, config)
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
      if (logger.isTraceEnabled) logger.trace("registerNoNewEvents - Rescheduled to next {} milliseconds", nextInterval)
    }

    fun registerFailure(throwable: Throwable) {
      greedy.set(false)
      val jitter = config.jitterFunction.invoke()
      val nextInterval = min(config.maxInterval, (config.interval * failures.incrementAndGet()) + jitter)
      crabzillaContext.vertx.setTimer(nextInterval, handler())
      logger.error("registerFailure - Rescheduled to next {} milliseconds", nextInterval, throwable)
    }

    fun registerSuccess(eventSequence: Long) {
      currentOffset = eventSequence
      failures.set(0)
      backOff.set(0)
      val nextInterval = if (greedy.get()) GREED_INTERVAL else config.interval
      crabzillaContext.vertx.setTimer(nextInterval, handler())
      if (logger.isTraceEnabled) logger.trace("registerSuccess - Rescheduled to next {} milliseconds", nextInterval)
    }

    fun justReschedule() {
      crabzillaContext.vertx.setTimer(config.interval, handler())
      if (logger.isTraceEnabled) logger.trace("It is busy=$isBusy or paused=$isPaused. Will just reschedule.")
      if (logger.isTraceEnabled) logger.trace("justReschedule - Rescheduled to next {} milliseconds", config.interval)
    }

    fun publish(
      subscriptionDao: SubscriptionDao,
      eventsList: List<EventRecord>,
    ): Future<Void> {
      if (logger.isTraceEnabled) {
        logger.trace(
          "Found {} new events. The first is {} and last is {}",
          eventsList.size,
          eventsList.first().metadata.eventSequence,
          eventsList.last().metadata.eventSequence,
        )
      }

      return ViewEffectHandler(subscriptionDao.sqlConnection, viewEffect, viewTrigger)
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
          if (logger.isTraceEnabled) logger.trace("Scanning for new events for subscription {}", spec.subscriptionName)
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

  companion object {
    private val node: String = ManagementFactory.getRuntimeMXBean().name
    private const val GREED_INTERVAL = 100L
  }
}
