package io.github.crabzilla.core.test

import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.command.CommandException.ValidationException
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.CommandSession

/**
 * A helper for basic specifications
 */
class TestSpecification<S : Any, C : Any, E : Any>(val config: CommandControllerConfig<S, C, E>) {

  private var state: S? = null
  private val events: MutableList<E> = mutableListOf() // TODO replace with AppendedEvents (to get EventMetadata)

  fun state(): S? = state
  fun events(): List<E> = events.toList()

  fun whenCommand(command: C): TestSpecification<S, C, E> {
    if (config.commandValidator != null) {
      val validationErrors = config.commandValidator.validate(command)
      if (validationErrors.isNotEmpty()) {
        throw ValidationException(validationErrors)
      }
    }
    val commandHandler: CommandHandler<S, C, E> = config.commandHandlerFactory.invoke()
    val session: CommandSession<S, E> = commandHandler.handleCommand(command, state)
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
