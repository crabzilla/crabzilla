package io.github.crabzilla.core

class CommandsSession<C, S, E>(
  originalState: S,
  private val eventHandler: (S, E) -> S,
  private val commandHandler: (S, C) -> List<E>,
) {
  private var currentState: S = originalState
  private val appliedEvents = mutableListOf<E>()

  fun handle(command: C): CommandsSession<C, S, E> {
    val resultingEvents = commandHandler.invoke(currentState, command)
    apply(resultingEvents)
    return this
  }

  fun apply(events: List<E>): CommandsSession<C, S, E> {
    events.forEach { domainEvent ->
      currentState = eventHandler.invoke(currentState, domainEvent)
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
