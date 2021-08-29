package io.github.crabzilla.stack.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandHandlerApi
import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.command.EventHandler
import io.vertx.core.Future

/**
 * To handle commands returning Future
 */
abstract class FutureCommandHandler<S : State, C : Command, E : Event>(handler: EventHandler<S, E>) :
  CommandHandlerApi<S, C, E>(handler) {

  fun <S : State, E : Event> CommandSession<S, E>.toFuture(): Future<CommandSession<S, E>> {
    return Future.succeededFuture(this)
  }

  abstract fun handleCommand(
    command: C,
    state: S?
  ): Future<CommandSession<S, E>>
}
