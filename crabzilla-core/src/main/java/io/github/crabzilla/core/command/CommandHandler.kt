package io.github.crabzilla.core.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

/**
 * To handle commands
 */
abstract class CommandHandler<S : State, C : Command, E : Event>(private val applier: EventHandler<S, E>) {

  protected fun withNew(events: List<E>): CommandSession<S, E> {
    return CommandSession(events, applier)
  }
  protected fun with(state: S?): CommandSession<S, E> {
    return CommandSession(state!!, applier)
  }
  abstract fun handleCommand(command: C, state: S?): CommandSession<S, E>
}
