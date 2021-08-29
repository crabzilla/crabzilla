package io.github.crabzilla.core.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

/**
 * To handle commands
 */
abstract class CommandHandler<S : State, C : Command, E : Event>(applier: EventHandler<S, E>) :
  CommandHandlerApi<S, C, E>(applier) {
  abstract fun handleCommand(command: C, state: S?): StatefulSession<S, E>
}
