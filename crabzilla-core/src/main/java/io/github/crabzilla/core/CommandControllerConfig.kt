package io.github.crabzilla.core

/**
 * A configuration for a command controller
 */
class CommandControllerConfig<A : DomainState, C : Command, E : DomainEvent>(
  val name: String,
  val eventHandler: EventHandler<A, E>,
  val commandHandlerFactory: () -> CommandHandlerApi<A, C, E>,
  val commandValidator: CommandValidator<C>? = null
)
