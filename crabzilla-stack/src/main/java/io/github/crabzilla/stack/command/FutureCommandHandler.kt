package io.github.crabzilla.stack.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandHandlerApi
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.EventHandler
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.Future

/**
 * To handle commands returning Future
 */
interface FutureCommandHandler<A : DomainState, C : Command, E : DomainEvent> : CommandHandlerApi<A, C, E> {

  fun <A : DomainState, E : DomainEvent> StatefulSession<A, E>.toFuture(): Future<StatefulSession<A, E>> {
    return Future.succeededFuture(this)
  }

  fun handleCommand(
    command: C,
    eventHandler: EventHandler<A, E>,
    snapshot: Snapshot<A>?
  ): Future<StatefulSession<A, E>>
}
