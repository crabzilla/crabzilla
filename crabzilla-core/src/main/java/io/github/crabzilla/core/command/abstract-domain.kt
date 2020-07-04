package io.github.crabzilla.core.command

import io.vertx.core.Future
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
abstract class Command

@Serializable
@Polymorphic
open class DomainEvent

@Serializable
@Polymorphic
open class AggregateRoot

interface AggregateRootCommandAware<A : AggregateRoot> {
  val entityName: String
  val initialState: A
  val applyEvent: (event: DomainEvent, state: A) -> A
  val validateCmd: (command: Command) -> List<String>
  val handleCmd: (id: Int, state: A, command: Command) -> Future<List<DomainEvent>>
}
