package io.github.crabzilla.core

class Session<C, S, E>(
  private val initialState: S,
  private val evolveFunction: (S, E) -> S,
  private val decideFunction: (S, C) -> List<E>,
  private val injectorFunction: ((S) -> S)? = null,
) {
  private var currentState: S = initialState
  private val appliedEvents = mutableListOf<E>()

  fun reset(): Session<C, S, E> {
    this.appliedEvents.clear()
    this.currentState = initialState
    return this
  }

  fun decide(command: C): Session<C, S, E> {
    injectorFunction?.invoke(currentState)
    val resultingEvents = decideFunction.invoke(currentState, command)
    evolve(resultingEvents)
    return this
  }

  fun evolve(vararg events: E): Session<C, S, E> {
    return evolve(events.toList())
  }

  fun evolve(events: List<E>): Session<C, S, E> {
    events.forEach { domainEvent ->
      currentState = evolveFunction.invoke(currentState, domainEvent)
      appliedEvents.add(domainEvent)
      if (injectorFunction != null) {
        currentState = injectorFunction.invoke(currentState)
      }
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
