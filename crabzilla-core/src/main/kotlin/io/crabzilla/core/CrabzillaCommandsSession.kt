package io.crabzilla.core

class CrabzillaCommandsSession<C, S, E>(
  initialState: S,
  private val evolveFunction: (S, E) -> S,
  private val decideFunction: (S, C) -> List<E>,
) {
  private var currentState: S = initialState
  private val appliedEvents = mutableListOf<E>()

  fun handle(command: C): CrabzillaCommandsSession<C, S, E> {
    val resultingEvents = decideFunction.invoke(currentState, command)
    apply(resultingEvents)
    return this
  }

  fun apply(events: List<E>): CrabzillaCommandsSession<C, S, E> {
    events.forEach { domainEvent ->
      currentState = evolveFunction.invoke(currentState, domainEvent)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  fun currentState(): S {
    return currentState
  }

  fun appliedEvents(): List<E> {
    return appliedEvents
  }
}
