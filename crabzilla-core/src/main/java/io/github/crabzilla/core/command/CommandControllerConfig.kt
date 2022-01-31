package io.github.crabzilla.core.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

/**
 * A configuration for a command controller
 */
class CommandControllerConfig<S : State, C : Command, E : Event>(
  val name: String,
  val eventHandler: EventHandler<S, E>,
  val commandHandlerApiFactory: () -> CommandHandler<S, C, E>,
  val commandValidator: CommandValidator<C>? = null
)
