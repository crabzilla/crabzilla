package io.github.crabzilla.core

/**
 * To apply and track events against state
 */
class CommandSession<S, E>(state: S, private val eventHandler: EventHandler<S, E>) {

  private val originalState: S = state
  val appliedEvents = mutableListOf<E>()
  var currentState: S

  init {
    this.currentState = originalState
  }

  fun appliedEvents(): List<E> {
    return appliedEvents
  }

  fun apply(events: List<E>): CommandSession<S, E> {
    events.forEach { domainEvent ->
      apply(domainEvent)
    }
    return this
  }

  @Suppress
  fun apply(vararg event: E): CommandSession<S, E> {
    event.forEach {
      currentState = eventHandler.handle(currentState, it)
      appliedEvents.add(it)
    }
    return this
  }

  inline fun execute(fn: (S) -> List<E>): CommandSession<S, E> {
    val newEvents = fn.invoke(currentState)
    return apply(newEvents)
  }

  fun response(): Triple<S, List<E>, S> {
    return Triple(originalState, appliedEvents, currentState)
  }
}
