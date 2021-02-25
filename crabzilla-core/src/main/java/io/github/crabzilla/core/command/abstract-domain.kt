package io.github.crabzilla.core.command

import io.vertx.core.Future
import kotlinx.serialization.Serializable

@Serializable
abstract class Command

@Serializable
abstract class DomainEvent

@Serializable
abstract class AggregateRoot

interface AggregateRootCommandAware<A : AggregateRoot> {
  val entityName: String
  val initialState: A
  val applyEvent: (event: DomainEvent, state: A) -> A
  val validateCmd: (command: Command) -> List<String>
  val handleCmd: (id: Int, state: A, command: Command) -> Future<List<DomainEvent>>
}
