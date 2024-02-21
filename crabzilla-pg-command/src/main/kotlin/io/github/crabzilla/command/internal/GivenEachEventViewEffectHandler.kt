package io.github.crabzilla.command.internal

import io.github.crabzilla.command.CommandHandlerResult
import io.github.crabzilla.command.ViewEffect
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.ViewTrigger
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import org.slf4j.LoggerFactory

internal class GivenEachEventViewEffectHandler<S : Any, E : Any>(
  private val sqlConnection: SqlConnection,
  private val viewEffect: ViewEffect.GivenEachEventViewEffect<S, E>,
  private val viewTrigger: ViewTrigger? = null,
) : ViewEffectHandler<S, E> {
  override fun handle(commandHandlerResult: CommandHandlerResult<S, E>): Future<JsonObject?> {
    if (logger.isDebugEnabled) logger.debug("Will project {} events ", commandHandlerResult.events.size)
    val results =
      commandHandlerResult.metadata.mapIndexed { index, metadata ->
        Pair(commandHandlerResult.events[index], metadata)
      }
    val initialFuture = Future.succeededFuture<JsonObject?>()
    return results.fold(
      initialFuture,
    ) { currentFuture: Future<JsonObject?>, appendedEvent: Pair<E, EventMetadata> ->
      currentFuture.compose {
        viewEffect.handle(sqlConnection, appendedEvent.first, commandHandlerResult.snapshot.state, appendedEvent.second)
      }
    }
      .onSuccess { viewAsJson ->
        if (viewAsJson != null && viewTrigger != null) {
          if (viewTrigger.checkCondition(viewAsJson)) {
            viewTrigger.handleTrigger(sqlConnection, viewAsJson)
          }
        }
      }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(GivenEachEventViewEffectHandler::class.java)
  }
}
