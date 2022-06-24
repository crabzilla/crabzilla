package io.github.crabzilla.stack.command

import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.core.EventHandler
import kotlin.reflect.KClass

/**
 * A configuration for a feature
 */
class CommandServiceConfig<S : Any, C : Any, E : Any>(
  val stateClass: KClass<S>,
  val commandClass: KClass<C>,
  val eventClass: KClass<E>,
  val eventHandler: EventHandler<S, E>,
  val commandHandler: CommandHandler<S, C, E>
)
