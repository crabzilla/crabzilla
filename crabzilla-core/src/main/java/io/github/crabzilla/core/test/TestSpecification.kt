package io.github.crabzilla.core.test

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandException.ValidationException
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.Snapshot

/**
 * A helper for basic specifications
 */
class TestSpecification<S : State, C : Command, E : Event>(val config: CommandControllerConfig<S, C, E>) {

  private var state: S? = null
  private val events: MutableList<E> = mutableListOf()

  fun state(): S? = state
  fun events(): List<E> = events.toList()

  fun whenCommand(command: C): TestSpecification<S, C, E> {
    if (config.commandValidator != null) {
      val validationErrors = config.commandValidator.validate(command)
      if (validationErrors.isNotEmpty()) {
        throw ValidationException(validationErrors)
      }
    }
    val commandHandler = config.commandHandlerFactory.invoke() as CommandHandler<S, C, E>
    val snapshot = if (state == null) null else Snapshot(state!!, events.size)
    val session = commandHandler.handleCommand(command, snapshot)
    state = session.currentState
    events.addAll(session.appliedEvents())
    return this
  }

  fun then(assertion: (s: TestSpecification<S, C, E>) -> Unit): TestSpecification<S, C, E> {
    assertion.invoke(this)
    return this
  }

  fun givenEvents(vararg fixtureEvents: E): TestSpecification<S, C, E> {
    fixtureEvents.forEach { e ->
      val newState = config.eventHandler.handleEvent(state, e)
      state = newState
      events.add(e)
    }
    return this
  }
}
