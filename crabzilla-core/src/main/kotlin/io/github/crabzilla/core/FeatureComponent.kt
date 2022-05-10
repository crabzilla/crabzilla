package io.github.crabzilla.core

import kotlin.reflect.KClass

/**
 * A configuration for a feature
 */
class FeatureComponent<S : Any, C : Any, E : Any>(
  private val stateClass: KClass<S>,
  val commandClass: KClass<C>,
  val eventClass: KClass<E>,
  val eventHandler: EventHandler<S, E>,
  val commandHandlerFactory: () -> CommandHandler<S, C, E>,
  val commandValidator: CommandValidator<C>? = null
// TODO Consider using QuerySpecification to query and project just some events types to your feature state
) {
  fun stateClassName() = stateClass.simpleName!!
}
