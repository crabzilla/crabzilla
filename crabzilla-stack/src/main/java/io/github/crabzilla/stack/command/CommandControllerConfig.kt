package io.github.crabzilla.stack.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandHandlerApi
import io.github.crabzilla.core.CommandValidator
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.EventHandler

/**
 * A configuration for a command controller
 */
class CommandControllerConfig<A : DomainState, C : Command, E : DomainEvent>(
  val name: String,
  val eventHandler: EventHandler<A, E>,
  val commandHandler: CommandHandlerApi<A, C, E>,
  val commandValidator: CommandValidator<C>? = null
)
