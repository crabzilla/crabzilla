package io.github.crabzilla.context

import io.github.crabzilla.context.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.sqlclient.Pool
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class PgNotifierVerticle(
  private val pgPool: Pool,
  private val intervalMilliseconds: Long = 10_000,
) : AbstractVerticle() {
  private val notifications: MutableSet<String> = ConcurrentHashMap.newKeySet()

  override fun start() {
    fun notify(): Future<Void> {
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
            .onSuccess { logger.debug("Notified postgres: $query") }
            .onFailure { logger.error("Error when notifying Postgres", it) }
        }
      }
    }

    vertx.eventBus().consumer(PG_NOTIFIER_ADD_ENDPOINT) {
      notifications.add(it.body())
    }
    vertx.setPeriodic(intervalMilliseconds) {
      notify().onSuccess { notifications.clear() }
    }
  }

  companion object {
    const val PG_NOTIFIER_ADD_ENDPOINT: String = "PG_NOTIFIER_ADD_ENDPOINT"
    private val logger = LoggerFactory.getLogger(PgNotifierVerticle::class.java)
  }
}
