package io.github.crabzilla.core.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

/**
 * To perform aggregate root business methods and track it's events and state
 */
class StatefulSession<S : State, E : Event> {
  val originalVersion: Int
  private val originalState: S
  private val eventHandler: EventHandler<S, E>
  private val appliedEvents = mutableListOf<E>()
  var currentState: S

  constructor(version: Int, state: S, eventHandler: EventHandler<S, E>) {
    this.originalVersion = version
    this.originalState = state
    this.eventHandler = eventHandler
    this.currentState = originalState
  }

  constructor(events: List<E>, eventHandler: EventHandler<S, E>) {
    this.originalVersion = 0
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

  fun apply(events: List<E>): StatefulSession<S, E> {
    events.forEach { domainEvent ->
      currentState = eventHandler.handleEvent(currentState, domainEvent)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  inline fun execute(fn: (S) -> List<E>): StatefulSession<S, E> {
    val newEvents = fn.invoke(currentState)
    return apply(newEvents)
  }

  fun register(event: E): StatefulSession<S, E> {
    return apply(listOf(event))
  }

  fun toSessionData(): SessionData {
    return SessionData(originalVersion, if (originalVersion == 0) null else originalState, appliedEvents, currentState)
  }

  data class SessionData(
    val originalVersion: Int,
    val originalState: State?,
    val events: List<Event>,
    val newState: State
  )
}
