package io.github.crabzilla.subscription.internal

import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.ViewTrigger
import io.github.crabzilla.subscription.SubscriptionApiViewEffect
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import org.slf4j.LoggerFactory

internal class ViewEffectHandler(
  private val sqlConnection: SqlConnection,
  private val viewEffect: SubscriptionApiViewEffect,
  private val viewTrigger: ViewTrigger? = null,
) {
  fun handle(appendedEvents: List<EventRecord>): Future<JsonObject?> {
    if (logger.isDebugEnabled) logger.debug("Will project {} events", appendedEvents.size)
    val initialFuture = Future.succeededFuture<JsonObject?>()
    return appendedEvents.fold(
      initialFuture,
    ) { currentFuture: Future<JsonObject?>, appendedEvent: EventRecord ->
      currentFuture.compose {
        viewEffect.handle(sqlConnection, appendedEvent)
      }
    }.onSuccess { viewAsJson ->
      if (viewAsJson != null && viewTrigger != null) {
        if (viewTrigger.checkCondition(viewAsJson)) {
          viewTrigger.handleTrigger(sqlConnection, viewAsJson)
        }
      }
    }
  }
  companion object {
    private val logger = LoggerFactory.getLogger(ViewEffectHandler::class.java)
  }
}
