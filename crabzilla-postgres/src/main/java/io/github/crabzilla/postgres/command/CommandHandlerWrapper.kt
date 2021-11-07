package io.github.crabzilla.postgres.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.CommandHandlerApi
import io.github.crabzilla.core.command.CommandSession
import io.vertx.core.Future

object CommandHandlerWrapper {

  fun <S : State, C : Command, E : Event> wrap(handler: CommandHandlerApi<S, C, E>):
    (command: C, state: S?) -> Future<CommandSession<S, E>> {
    return when (handler) {
      is CommandHandler<S, C, E> -> { command, state ->
        try {
          val session = handler.handleCommand(command, state)
          Future.succeededFuture(session)
        } catch (e: RuntimeException) {
          Future.failedFuture(e)
        }
      }
      is FutureCommandHandler<S, C, E> -> { command, state ->
        handler.handleCommand(command, state)
      }
      else -> throw UnknownCommandHandler("Unknown command handler: " + handler.javaClass.simpleName)
    }
  }
}
