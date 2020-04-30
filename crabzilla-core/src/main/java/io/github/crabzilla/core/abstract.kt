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
open class Entity

interface EntityCommandAware<E : Entity> {
  val initialState: E
  val applyEvent: (event: DomainEvent, state: E) -> E
  val validateCmd: (command: Command) -> List<String>
  val handleCmd: (id: Int, state: E, context: Command) -> Future<List<DomainEvent>>
}
