package io.github.crabzilla.framework

interface EntityCommandAware<E: Entity> {

  val initialState: E

  val applyEvent: (event: DomainEvent, state: E) -> E

  val validateCmd: (command: Command) -> List<String>

  val cmdHandlerFactory: (cmdMetadata: CommandMetadata, command: Command, snapshot: Snapshot<E>)
                                                        -> EntityCommandHandler<E>

}
