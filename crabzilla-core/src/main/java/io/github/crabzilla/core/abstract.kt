package io.github.crabzilla.core

import io.vertx.core.Future
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
open class Command

@Serializable
@Polymorphic
open class DomainEvent

@Serializable
@Polymorphic
open class Entity {
  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }
}

interface EntityCommandAware<E : Entity> {
  val initialState: E
  val applyEvent: (event: DomainEvent, state: E) -> E
  val validateCmd: (command: Command) -> List<String>
  val handleCmd: (request: Triple<CommandMetadata, Command, Snapshot<E>>) -> Future<List<DomainEvent>>
}
