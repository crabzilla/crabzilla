package io.github.crabzilla.core.command

import kotlin.reflect.KClass

/**
 * A configuration for a command controller
 */
class CommandControllerConfig<S : Any, C : Any, E : Any>(
  val stateClass: KClass<S>,
  val commandClass: KClass<C>,
  val eventClass: KClass<E>,
  val eventHandler: EventHandler<S, E>,
  val commandHandlerFactory: () -> CommandHandler<S, C, E>,
  val commandValidator: CommandValidator<C>? = null
)
