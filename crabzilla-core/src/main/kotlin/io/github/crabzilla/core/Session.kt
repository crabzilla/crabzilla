package io.github.crabzilla.core

class Session<C, S, E>(
  private val initialState: S,
  private val evolveFunction: (S, E) -> S,
  private val decideFunction: (S, C) -> List<E>,
  private val injectFunction: ((S) -> S)? = null,
) {
  private var currentState: S =
    if (injectFunction == null) initialState else injectFunction.invoke(initialState)
  private val appliedEvents = mutableListOf<E>()

  fun reset(): Session<C, S, E> {
    this.appliedEvents.clear()
    this.currentState = initialState
    return this
  }

  fun decide(command: C): Session<C, S, E> {
    val resultingEvents = decideFunction.invoke(currentState, command)
    evolve(resultingEvents)
    return this
  }

  fun evolve(vararg events: E): Session<C, S, E> {
    return evolve(events.toList())
  }

  private fun evolve(events: List<E>): Session<C, S, E> {
    events.forEach { domainEvent ->
      currentState = evolveFunction.invoke(currentState, domainEvent)
      appliedEvents.add(domainEvent)
      if (injectFunction != null) {
        currentState = injectFunction.invoke(currentState)
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
