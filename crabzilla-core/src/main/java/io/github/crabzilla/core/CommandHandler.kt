package io.github.crabzilla.core

/**
 * To handle commands
 */
interface CommandHandler<A : DomainState, C : Command, E : DomainEvent> : CommandHandlerApi<A, C, E> {

  fun handleCommand(command: C, eventHandler: EventHandler<A, E>, snapshot: Snapshot<A>?): StatefulSession<A, E>
}
