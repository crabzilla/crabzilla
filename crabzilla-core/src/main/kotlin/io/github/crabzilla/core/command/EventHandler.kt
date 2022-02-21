package io.github.crabzilla.core.command

/**
 * To apply an event to an aggregate root state
 */
fun interface EventHandler<S, E> {
  fun handleEvent(state: S?, event: E): S
}
