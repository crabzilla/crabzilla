package io.github.crabzilla.handler

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.EventProjector
import io.github.crabzilla.context.JsonObjectSerDer
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import java.util.*

interface CrabzillaHandler<C : Any> {
  fun handle(
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata? = null,
  ): Future<EventMetadata>

  fun handleWithinTransaction(
    conn: SqlConnection,
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata? = null,
  ): Future<EventMetadata>
}

data class TargetStream(
  val stateType: String,
  val stateId: String,
  val name: String = "$stateType@$stateId",
  val mustBeNew: Boolean = false,
)

data class CommandMetadata(
  val id: UUID = UUID.randomUUID(),
  val metadata: JsonObject? = null,
)

data class CrabzillaHandlerConfig<S : Any, C : Any, E : Any>(
  val initialState: S,
  val eventHandler: (S, E) -> S,
  val commandHandler: (S, C) -> List<E>,
  val eventSerDer: JsonObjectSerDer<E>,
  val commandSerDer: JsonObjectSerDer<C>? = null,
  val eventProjector: EventProjector? = null,
)

sealed class CommanderException(override val message: String, override val cause: Throwable? = null) :
  RuntimeException(message, cause) {
  class StreamMustBeNewException(message: String) : CommanderException(message)

  class StreamCantBeLockedException(message: String) : CommanderException(message)

  class BusinessException(message: String, cause: Throwable) : CommanderException(message, cause)
}
