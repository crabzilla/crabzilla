package io.github.crabzilla.core

import io.github.crabzilla.core.CommandException.ValidationException

/**
 * A helper for basic specifications
 */
class TestSpecification<A : DomainState, C : Command, E : DomainEvent>(val config: CommandControllerConfig<A, C, E>) {

  private var state: A? = null
  private val events: MutableList<E> = mutableListOf()

  fun state(): A? = state
  fun events(): List<E> = events.toList()

  fun whenCommand(command: C): TestSpecification<A, C, E> {
    if (config.commandValidator != null) {
      val validationErrors = config.commandValidator.validate(command)
      if (validationErrors.isNotEmpty()) {
        throw ValidationException(validationErrors)
      }
    }
    val commandHandler = config.commandHandlerFactory.invoke() as CommandHandler<A, C, E>
    val snapshot = if (state == null) null else Snapshot(state!!, events.size)
    val session = commandHandler.handleCommand(command, snapshot)
    state = session.currentState
    events.addAll(session.appliedEvents())
    return this
  }

  fun then(assertion: (s: TestSpecification<A, C, E>) -> Unit): TestSpecification<A, C, E> {
    assertion.invoke(this)
    return this
  }

  fun givenEvents(vararg fixtureEvents: E): TestSpecification<A, C, E> {
    fixtureEvents.forEach { e ->
//      println("state $state event $e")
      val newState = config.eventHandler.handleEvent(state, e)
//      println("new state $newState")
      state = newState
      events.add(e)
    }
    return this
  }
}
