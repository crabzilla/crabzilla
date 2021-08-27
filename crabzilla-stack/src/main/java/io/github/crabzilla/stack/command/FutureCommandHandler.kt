package io.github.crabzilla.stack.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandHandlerApi
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.core.command.Snapshot
import io.github.crabzilla.core.command.StatefulSession
import io.vertx.core.Future

/**
 * To handle commands returning Future
 */
abstract class FutureCommandHandler<S : State, C : Command, E : Event>(handler: EventHandler<S, E>) :
  CommandHandlerApi<S, C, E>(handler) {

  fun <A : State, E : Event> StatefulSession<A, E>.toFuture(): Future<StatefulSession<A, E>> {
    return Future.succeededFuture(this)
  }

  abstract fun handleCommand(
    command: C,
    snapshot: Snapshot<S>?
  ): Future<StatefulSession<S, E>>
}
