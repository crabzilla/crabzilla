package io.github.crabzilla.core

interface CommandHandlerApi<A : DomainState, C : Command, E : DomainEvent> {

  fun <A : DomainState, E : DomainEvent> withNew(events: List<E>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
    return StatefulSession(events, applier)
  }

  fun <A : DomainState, E : DomainEvent> with(snapshot: Snapshot<A>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
    return StatefulSession(snapshot.version, snapshot.state, applier)
  }
}
