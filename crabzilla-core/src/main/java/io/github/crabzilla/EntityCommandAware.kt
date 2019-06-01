package io.github.crabzilla

interface EntityCommandAware<E: Entity> {

  fun initialState(): E

  fun applyEvent(event: DomainEvent, state: E): E

  fun validateCmd(command: Command): List<String>

  fun cmdHandlerFactory(): CommandHandlerFactory<E>

}
