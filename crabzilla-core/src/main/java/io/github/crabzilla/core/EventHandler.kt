package io.github.crabzilla.core

/**
 * To apply an event to an aggregate root state
 */
fun interface EventHandler<A : DomainState, E : DomainEvent> {
  fun handleEvent(state: A?, event: E): A
}
