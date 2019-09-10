package io.github.crabzilla.framework

interface EntityCommandAware<E: Entity> {

  fun initialState(): E

  fun applyEvent(event: DomainEvent, state: E): E

  fun validateCmd(command: Command): List<String>

  fun cmdHandlerFactory(): EntityCommandHandlerFactory<E>

}
