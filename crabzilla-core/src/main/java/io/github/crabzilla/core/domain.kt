package io.github.crabzilla.core

import io.vertx.core.Future
import java.util.UUID
import kotlinx.serialization.Serializable

// abstract domain

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

// concrete domain

typealias Version = Int

data class Snapshot<A : AggregateRoot>(
  val state: A,
  val version: Version
)

data class UnitOfWork(
  val entityName: String,
  val aggregateRootId: Int,
  val commandId: UUID,
  val command: Command,
  val version: Version,
  val events: List<DomainEvent>
) {
  init {
    require(this.version >= 1) { "version must be >= 1" }
    require(this.events.size <= 6) { "only 6 events are supported so far" }
  }
}
