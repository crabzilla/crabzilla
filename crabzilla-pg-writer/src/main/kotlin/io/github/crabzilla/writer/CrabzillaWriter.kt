package io.github.crabzilla.writer

import io.github.crabzilla.context.CrabzillaRuntimeException
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.context.ViewEffect
import io.github.crabzilla.context.ViewTrigger
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import java.util.*

data class CommandMetadata(
  val commandId: UUID = UUID.randomUUID(),
  val metadata: JsonObject? = null,
)

interface CrabzillaWriter<C : Any> {
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

data class CrabzillaWriterConfig<S : Any, C : Any, E : Any>(
  val initialState: S,
  val eventHandler: (S, E) -> S,
  val commandHandler: (S, C) -> List<E>,
  val eventSerDer: JsonObjectSerDer<E>,
  val commandSerDer: JsonObjectSerDer<C>? = null,
  // TODO consider an optional state serder (on stream module (to migrate the stream))
  val viewEffect: ViewEffect? = null,
  val viewTrigger: ViewTrigger? = null,
)

class BusinessException(message: String, cause: Throwable) : CrabzillaRuntimeException(message, cause)
