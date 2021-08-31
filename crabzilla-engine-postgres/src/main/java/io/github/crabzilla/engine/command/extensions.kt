package io.github.crabzilla.engine.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.CommandHandlerApi
import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.stack.command.FutureCommandHandler
import io.vertx.core.Future

fun <S : State, C : Command, E : Event> CommandHandlerApi<S, C, E>.wrap(): (command: C, state: S?) -> Future<CommandSession<S, E>> {
  return when (val handler: CommandHandlerApi<S, C, E> = this) {
    is CommandHandler<S, C, E> -> { command, state ->
      try {
        val session = handler.handleCommand(command, state)
        Future.succeededFuture(session)
      } catch (e: Throwable) {
        Future.failedFuture(e)
      }
    }
    is FutureCommandHandler<S, C, E> -> { command, state ->
      handler.handleCommand(command, state)
    }
    else -> throw UnknownCommandHandler("Unknown command handler: " + handler.javaClass.simpleName)
  }
}
