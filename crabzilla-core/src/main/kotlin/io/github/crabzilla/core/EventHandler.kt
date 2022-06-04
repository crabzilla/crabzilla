package io.github.crabzilla.core

/**
 * To apply an event to an aggregate root state
 */
fun interface EventHandler<S, E> {
  fun handle(state: S?, event: E): S
}
