package io.github.crabzilla

interface EntityStateFunctions<E: Entity> {

  fun initialState(): E

  fun applyEvent(event: DomainEvent, state: E): E

}
