package io.crabzilla.writer

import io.crabzilla.context.CrabzillaRuntimeException
import io.crabzilla.context.EventMetadata
import io.crabzilla.context.JsonObjectSerDer
import io.crabzilla.context.ViewTrigger
import io.crabzilla.stream.StreamSnapshot
import io.crabzilla.stream.TargetStream
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import java.util.*

// to be implemented by models

interface InitialStateFactory<S : Any> {
  fun get(): S
}

sealed interface ViewEffect<S : Any, E : Any> {
  interface EventStateViewEffect<S : Any, E : Any> : ViewEffect<S, E> {
    fun handle(
      sqlConnection: SqlConnection,
      event: E,
      state: S,
      eventMetadata: EventMetadata,
    ): Future<JsonObject?>
  }

  interface WriteResultViewEffect<S : Any, E : Any> : ViewEffect<S, E> {
    fun handle(
      sqlConnection: SqlConnection,
      result: WriteResult<S, E>,
    ): Future<JsonObject?>
  }
}

// crabzilla api

data class CommandMetadata(
  val commandId: UUID = UUID.randomUUID(),
  val metadata: JsonObject? = null,
)

data class WriteResult<S : Any, E : Any>(
  val snapshot: StreamSnapshot<S>,
  val events: List<E>,
  val metadata: List<EventMetadata>,
)

interface WriterApi<S : Any, C : Any, E : Any> {
  fun withinTransaction(commandOperation: (SqlConnection) -> Future<WriteResult<S, E>>): Future<WriteResult<S, E>>

  fun handle(
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata = CommandMetadata(),
  ): Future<WriteResult<S, E>>

  fun handleWithinTransaction(
    sqlConnection: SqlConnection,
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata = CommandMetadata(),
  ): Future<WriteResult<S, E>>
}

data class WriterConfig<S : Any, C : Any, E : Any>(
  val initialStateFactory: InitialStateFactory<S>,
  val evolveFunction: (S, E) -> S,
  val decideFunction: (S, C) -> List<E>,
  val eventSerDer: JsonObjectSerDer<E>,
  val commandSerDer: JsonObjectSerDer<C>? = null,
  val viewEffect: ViewEffect<S, E>? = null,
  val viewTrigger: ViewTrigger? = null,
  val persistEvents: Boolean? = true,
  val persistCommands: Boolean? = commandSerDer != null,
  val notifyPostgres: Boolean = true,
)

class BusinessException(message: String, cause: Throwable) : CrabzillaRuntimeException(message, cause)
