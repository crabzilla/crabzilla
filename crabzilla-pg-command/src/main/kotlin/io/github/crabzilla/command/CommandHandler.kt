package io.github.crabzilla.command

import io.github.crabzilla.stream.TargetStream
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

interface CommandHandler<S : Any, C : Any, E : Any> {
  fun withinTransaction(commandOperation: (SqlConnection) -> Future<CommandHandlerResult<S, E>>): Future<CommandHandlerResult<S, E>>

  fun handle(
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata = CommandMetadata(),
  ): Future<CommandHandlerResult<S, E>>

  fun handleWithinTransaction(
    sqlConnection: SqlConnection,
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata = CommandMetadata(),
  ): Future<CommandHandlerResult<S, E>>
}
