package io.github.crabzilla.core

interface CommandHandlerApi<A : DomainState, C : Command, E : DomainEvent> {

  class ConstructorResult<A, E>(val state: A, vararg val events: E)

  fun <A : DomainState, E : DomainEvent> withNew(create: ConstructorResult<A, E>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
    return StatefulSession(create, applier)
  }

  fun <A : DomainState, E : DomainEvent> with(snapshot: Snapshot<A>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
    return StatefulSession(snapshot.version, snapshot.state, applier)
  }
}
