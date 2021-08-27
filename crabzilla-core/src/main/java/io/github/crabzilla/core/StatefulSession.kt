package io.github.crabzilla.core

/**
 * To perform aggregate root business methods and track it's events and state
 */
class StatefulSession<A : DomainState, E : DomainEvent> {
  private val originalVersion: Int
  private val originalState: A
  private val eventHandler: EventHandler<A, E>
  private val appliedEvents = mutableListOf<E>()
  var currentState: A

  constructor(version: Int, state: A, eventHandler: EventHandler<A, E>) {
    this.originalVersion = version
    this.originalState = state
    this.eventHandler = eventHandler
    this.currentState = originalState
  }

  constructor(events: List<E>, eventHandler: EventHandler<A, E>) {
    this.originalVersion = 0
    this.eventHandler = eventHandler
    val state: A? = events.fold(null) { s: A?, e: E ->
      appliedEvents.add(e)
      eventHandler.handleEvent(s, e)
    }
    this.originalState = state!!
    this.currentState = originalState
  }

  fun appliedEvents(): List<E> {
    return appliedEvents
  }

  fun apply(events: List<E>): StatefulSession<A, E> {
    events.forEach { domainEvent ->
      currentState = eventHandler.handleEvent(currentState, domainEvent)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  inline fun execute(fn: (A) -> List<E>): StatefulSession<A, E> {
    val newEvents = fn.invoke(currentState)
    return apply(newEvents)
  }

  fun register(event: E): StatefulSession<A, E> {
    return apply(listOf(event))
  }

  fun toSessionData(): SessionData {
    return SessionData(originalVersion, if (originalVersion == 0) null else originalState, appliedEvents, currentState)
  }

  data class SessionData(
    val originalVersion: Int,
    val originalState: DomainState?,
    val events: List<DomainEvent>,
    val newState: DomainState
  )
}
