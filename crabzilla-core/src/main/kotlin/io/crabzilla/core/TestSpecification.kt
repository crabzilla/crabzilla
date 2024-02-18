package io.crabzilla.core

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
