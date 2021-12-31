package io.github.crabzilla.core.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

/**
 * To apply and track events against state
 */
class CommandSession<S : State, E : Event> {
  val originalState: S
  private val eventHandler: EventHandler<S, E>
  private val appliedEvents = mutableListOf<E>()
  var currentState: S

  constructor(state: S, eventHandler: EventHandler<S, E>) {
    this.originalState = state
    this.eventHandler = eventHandler
    this.currentState = originalState
  }

  constructor(events: List<E>, eventHandler: EventHandler<S, E>) {
    this.eventHandler = eventHandler
    val state: S? = events.fold(null) { s: S?, e: E ->
      appliedEvents.add(e)
      eventHandler.handleEvent(s, e)
    }
    this.originalState = state!!
    this.currentState = originalState
  }

  fun appliedEvents(): List<E> {
    return appliedEvents
  }

  fun apply(events: List<E>): CommandSession<S, E> {
    events.forEach { domainEvent ->
      currentState = eventHandler.handleEvent(currentState, domainEvent)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  inline fun execute(fn: (S) -> List<E>): CommandSession<S, E> {
    val newEvents = fn.invoke(currentState)
    return apply(newEvents)
  }

  fun register(event: E): CommandSession<S, E> {
    return apply(listOf(event))
  }

  fun toSessionData(): CommandSessionData {
    return CommandSessionData(originalState, appliedEvents, currentState)
  }
}
