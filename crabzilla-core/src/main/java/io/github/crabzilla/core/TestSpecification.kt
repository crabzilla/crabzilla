package io.github.crabzilla.core

/**
 * A helper for basic specifications
 */
class TestSpecification<A : DomainState, C : Command, E : DomainEvent>(val config: CommandControllerConfig<A, C, E>) {

  private var state: A? = null
  private val events: MutableList<E> = mutableListOf()

  fun state(): A? = state
  fun events(): List<E> = events.toList()

  fun whenCommand(command: C): TestSpecification<A, C, E> {
    val commandHandler = config.commandHandler as CommandHandler<A, C, E>
    val snapshot = if (state == null) null else Snapshot(state!!, events.size)
    val session = commandHandler.handleCommand(command, config.eventHandler, snapshot)
    state = session.currentState
    events.addAll(session.appliedEvents())
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
