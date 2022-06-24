package io.github.crabzilla.core

/**
 * A helper for basic test specifications
 */
class CommandTestSpecification<S : Any, C : Any, E : Any>(
  private val commandHandler: CommandHandler<S, C, E>,
  private val eventHandler: EventHandler<S, E>
) {

  private var state: S? = null
  private val events: MutableList<E> = mutableListOf()

  fun state(): S? = state
  fun events(): List<E> = events.toList()

  fun whenCommand(command: C): CommandTestSpecification<S, C, E> {
    val session: CommandSession<S, E> = commandHandler.handle(command, state)
    state = session.currentState
    events.addAll(session.appliedEvents())
    return this
  }

  fun then(assertion: (s: CommandTestSpecification<S, C, E>) -> Unit): CommandTestSpecification<S, C, E> {
    assertion.invoke(this)
    return this
  }

  fun givenEvents(vararg fixtureEvents: E): CommandTestSpecification<S, C, E> {
    fixtureEvents.forEach { e ->
      val newState = eventHandler.handle(state, e)
      state = newState
      events.add(e)
    }
    return this
  }
}
