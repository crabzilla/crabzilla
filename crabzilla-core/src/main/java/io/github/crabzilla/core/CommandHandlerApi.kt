package io.github.crabzilla.core

open class CommandHandlerApi<A : DomainState, C : Command, E : DomainEvent>(
  private val applier: EventHandler<A, E>
) {
  fun withNew(events: List<E>): StatefulSession<A, E> {
    return StatefulSession(events, applier)
  }
  fun with(snapshot: Snapshot<A>): StatefulSession<A, E> {
    return StatefulSession(snapshot.version, snapshot.state, applier)
  }
}
