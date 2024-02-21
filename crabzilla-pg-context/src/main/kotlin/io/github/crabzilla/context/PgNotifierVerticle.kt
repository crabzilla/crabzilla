package io.github.crabzilla.context

import io.github.crabzilla.context.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.sqlclient.Pool
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class PgNotifierVerticle(
  private val pgPool: Pool,
  private val intervalMilliseconds: Long = 1_000,
  private val maxInterval: Long = 60_000,
) : AbstractVerticle() {
  private val notifications: MutableSet<String> = ConcurrentHashMap.newKeySet()
  private val backOff = AtomicLong(0L)
  private val jitterFunction: () -> Int = { ((0..10).random() * 1000) }

  val handler =
    Handler<Long> {
      if (notifications.size == 0) {
        backOff.incrementAndGet()
        if (logger.isDebugEnabled) logger.debug("Nothing to notify")
      } else {
        if (logger.isDebugEnabled) logger.debug("Will notify {} elements", notifications.size)
        notify().onSuccess { notifications.clear() }
      }
      schedule()
    }

  private fun schedule() {
    val jitter = jitterFunction.invoke()
    val nextInterval = min(maxInterval, intervalMilliseconds * backOff.incrementAndGet() + jitter)
    vertx.setTimer(nextInterval, handler)
    if (logger.isTraceEnabled) logger.trace("Rescheduled to next {} milliseconds", nextInterval)
  }

  private fun notify(): Future<Void> {
    val initialFuture = Future.succeededFuture<Void>()
    val list =
      notifications
        .map { stateType -> "NOTIFY $POSTGRES_NOTIFICATION_CHANNEL, '$stateType'" }
    return list.fold(
      initialFuture,
    ) { currentFuture: Future<Void>, query: String ->
      currentFuture.compose {
        pgPool.preparedQuery(query).execute()
          .mapEmpty<Void>()
          .onSuccess { if (logger.isDebugEnabled) logger.debug("Notified postgres: $query") }
          .onFailure { logger.error("Error when notifying Postgres", it) }
      }
    }
  }

  override fun start() {
    vertx.eventBus().consumer<String>(PG_NOTIFIER_ADD_ENDPOINT) {
      if (logger.isDebugEnabled) logger.debug("Received {}", it.body())
      notifications.add(it.body())
    }

    vertx.setTimer(intervalMilliseconds, handler)
    if (logger.isDebugEnabled) logger.debug("Rescheduled to next {} milliseconds", intervalMilliseconds)
  }

  companion object {
    const val PG_NOTIFIER_ADD_ENDPOINT: String = "PG_NOTIFIER_ADD_ENDPOINT"
    private val logger = LoggerFactory.getLogger(PgNotifierVerticle::class.java)
  }
}
