package io.github.crabzilla.core

class CrabzillaCommandsSession<C, S, E>(
  originalState: S,
  private val eventHandler: (S, E) -> S,
  private val commandHandler: (S, C) -> List<E>,
) {
  private var currentState: S = originalState
  private val appliedEvents = mutableListOf<E>()

  fun handle(command: C): CrabzillaCommandsSession<C, S, E> {
    val resultingEvents = commandHandler.invoke(currentState, command)
    apply(resultingEvents)
    return this
  }

  fun apply(events: List<E>): CrabzillaCommandsSession<C, S, E> {
    events.forEach { domainEvent ->
      currentState = eventHandler.invoke(currentState, domainEvent)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  fun currentState(): S {
    return currentState
  }

  fun appliedEvents(): List<E> {
    return appliedEvents
  }
}

fun <S, C> buildException(
  state: S,
  command: C,
): IllegalStateException {
  return IllegalStateException(
    "Illegal transition. " +
      "state: ${state!!::class.java.simpleName} command: ${command!!::class.java.simpleName}",
  )
}

class TestSpecification<C : Any, S : Any, E : Any>(
  private val session: CrabzillaCommandsSession<C, S, E>,
) {
  private var lastException: RuntimeException? = null

  fun whenCommand(command: C): TestSpecification<C, S, E> {
    try {
      session.handle(command)
    } catch (e: RuntimeException) {
      this.lastException = e
    }
    return this
  }

  fun then(assertion: (s: TestSpecification<C, S, E>) -> Unit): TestSpecification<C, S, E> {
    assertion.invoke(this)
    return this
  }

  fun givenEvents(vararg fixtureEvents: E): TestSpecification<C, S, E> {
    session.apply(fixtureEvents.toList())
    return this
  }

  fun currentState(): S {
    return session.currentState()
  }

  fun appliedEvents(): List<E> {
    return session.appliedEvents()
  }

  fun lastException(): RuntimeException? {
    return lastException
  }
}
