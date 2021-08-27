package io.github.crabzilla.core.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

/**
 * A configuration for a command controller
 */
class CommandControllerConfig<A : State, C : Command, E : Event>(
  val name: String,
  val eventHandler: EventHandler<A, E>,
  val commandHandlerFactory: () -> CommandHandlerApi<A, C, E>,
  val commandValidator: CommandValidator<C>? = null
)
