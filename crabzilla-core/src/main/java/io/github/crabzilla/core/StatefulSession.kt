package io.github.crabzilla.core

import io.github.crabzilla.core.CommandHandlerApi.ConstructorResult

/**
 * To perform aggregate root business methods and track it's events and state
 */
class StatefulSession<A : DomainState, E : DomainEvent> {
  val originalVersion: Int
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

  constructor(constructorResult: ConstructorResult<A, E>, eventHandler: EventHandler<A, E>) {
    this.originalVersion = 0
    this.originalState = constructorResult.state
    this.eventHandler = eventHandler
    this.currentState = originalState
    constructorResult.events.forEach {
      appliedEvents.add(it)
    }
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
