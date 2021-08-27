package io.github.crabzilla.core.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

open class CommandHandlerApi<S : State, C : Command, E : Event>(
  private val applier: EventHandler<S, E>
) {
  fun withNew(events: List<E>): StatefulSession<S, E> {
    return StatefulSession(events, applier)
  }
  fun with(snapshot: Snapshot<S>?): StatefulSession<S, E> {
    return StatefulSession(snapshot!!.version, snapshot.state, applier)
  }
}
