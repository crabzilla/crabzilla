package io.github.crabzilla.core

/**
 * A helper for basic test specifications
 */
class FeatureSpecification<S : Any, C : Any, E : Any>(private val featureComponent: FeatureComponent<S, C, E>) {

  private var state: S? = null
  private val events: MutableList<E> = mutableListOf()

  fun state(): S? = state
  fun events(): List<E> = events.toList()

  fun whenCommand(command: C): FeatureSpecification<S, C, E> {
    if (featureComponent.commandValidator != null) {
      val validationErrors = featureComponent.commandValidator.validate(command)
      if (validationErrors.isNotEmpty()) {
        throw IllegalArgumentException(validationErrors.toString())
      }
    }
    val commandHandler: CommandHandler<S, C, E> = featureComponent.commandHandlerFactory.invoke()
    val session: FeatureSession<S, E> = commandHandler.handleCommand(command, state)
    state = session.currentState
    events.addAll(session.appliedEvents())
    return this
  }

  fun then(assertion: (s: FeatureSpecification<S, C, E>) -> Unit): FeatureSpecification<S, C, E> {
    assertion.invoke(this)
    return this
  }

  fun givenEvents(vararg fixtureEvents: E): FeatureSpecification<S, C, E> {
    fixtureEvents.forEach { e ->
      val newState = featureComponent.eventHandler.handleEvent(state, e)
      state = newState
      events.add(e)
    }
    return this
  }
}
