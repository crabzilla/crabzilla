package io.github.crabzilla.core

/**
 * To handle commands
 */
abstract class CommandHandler<A : DomainState, C : Command, E : DomainEvent>(applier: EventHandler<A, E>) :
  CommandHandlerApi<A, C, E>(applier) {
  abstract fun handleCommand(command: C, snapshot: Snapshot<A>?): StatefulSession<A, E>
}
