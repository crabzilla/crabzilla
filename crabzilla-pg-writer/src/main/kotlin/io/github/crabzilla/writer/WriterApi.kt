package io.github.crabzilla.writer

import io.github.crabzilla.context.CrabzillaRuntimeException
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.context.ViewTrigger
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import org.slf4j.LoggerFactory
import java.util.*

data class CommandMetadata(
  val commandId: UUID = UUID.randomUUID(),
  val metadata: JsonObject? = null,
)

interface WriterApi<C : Any> {
  fun handle(
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata = CommandMetadata(),
  ): Future<EventMetadata>

  fun handleWithinTransaction(
    sqlConnection: SqlConnection,
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata = CommandMetadata(),
  ): Future<EventMetadata>
}

interface WriteApiEventViewEffect<E> {
  fun handle(
    sqlConnection: SqlConnection,
    eventMetadata: EventMetadata,
    event: E,
    // TODO consider a state param since it's available
  ): Future<JsonObject?>
}

data class WriterConfig<S : Any, C : Any, E : Any>(
  val initialState: S,
  val eventHandler: (S, E) -> S,
  val commandHandler: (S, C) -> List<E>,
  val eventSerDer: JsonObjectSerDer<E>,
  val commandSerDer: JsonObjectSerDer<C>? = null,
  // TODO consider an optional state serder (on stream module (to migrate the stream))
  val viewEffect: WriteApiEventViewEffect<E>? = null,
  val viewTrigger: ViewTrigger? = null,
)

class BusinessException(message: String, cause: Throwable) : CrabzillaRuntimeException(message, cause)

internal class EventProjector<E>(
  private val sqlConnection: SqlConnection,
  private val viewEffect: WriteApiEventViewEffect<E>,
  private val viewTrigger: ViewTrigger? = null,
) {
  fun handle(appendedEvents: List<Pair<EventMetadata, E>>): Future<JsonObject?> {
    logger.debug("Will project {} events", appendedEvents.size)
    logger.debug("Will project {}", appendedEvents)
    val initialFuture = Future.succeededFuture<JsonObject?>()
    return appendedEvents.fold(
      initialFuture,
    ) { currentFuture: Future<JsonObject?>, appendedEvent: Pair<EventMetadata, E> ->
      currentFuture.compose {
        viewEffect.handle(sqlConnection, appendedEvent.first, appendedEvent.second)
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
    private val logger = LoggerFactory.getLogger(EventProjector::class::java.name)
  }
}
