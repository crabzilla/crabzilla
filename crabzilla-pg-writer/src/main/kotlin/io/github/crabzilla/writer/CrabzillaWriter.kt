package io.github.crabzilla.writer

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.EventProjector
import io.github.crabzilla.context.JsonObjectSerDer
import io.github.crabzilla.context.TargetStream
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import java.util.*

interface CrabzillaWriter<C : Any> {
  fun handle(
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata? = null,
  ): Future<EventMetadata>

  fun handleWithinTransaction(
    sqlConnection: SqlConnection,
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata? = null,
  ): Future<EventMetadata>
}

data class CommandMetadata(
  val commandId: UUID = UUID.randomUUID(),
  val metadata: JsonObject? = null,
)

data class CrabzillaWriterConfig<S : Any, C : Any, E : Any>(
  val initialState: S,
  val eventHandler: (S, E) -> S,
  val commandHandler: (S, C) -> List<E>,
  val eventSerDer: JsonObjectSerDer<E>,
  val commandSerDer: JsonObjectSerDer<C>? = null,
  // TODO consider an optional state serder
  val eventProjector: EventProjector? = null,
)