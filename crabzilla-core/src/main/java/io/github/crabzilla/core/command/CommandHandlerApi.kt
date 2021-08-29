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
  fun with(state: S?): StatefulSession<S, E> {
    return StatefulSession(state!!, applier)
  }
}
