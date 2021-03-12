package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
abstract class DomainEvent

@Serializable
abstract class Command

@Serializable
abstract class AggregateRoot

// @Serializable
// abstract class ProcessManager

/**
 * To apply an event to an aggregate root state
 */
fun interface EventHandler<A : AggregateRoot, E : DomainEvent> {
  fun handleEvent(state: A?, event: E): A
}

/**
 * To handle commands
 */
interface CommandHandler<A : AggregateRoot, C : Command, E : DomainEvent> {

  class ConstructorResult<A, E>(val state: A, vararg val events: E)

  fun <A : AggregateRoot, E : DomainEvent> with(create: ConstructorResult<A, E>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
      return StatefulSession(create, applier)
    }

  fun <A : AggregateRoot, E : DomainEvent> with(snapshot: Snapshot<A>, applier: EventHandler<A, E>):
    StatefulSession<A, E> {
      return StatefulSession(snapshot.version, snapshot.state, applier)
    }

  fun handleCommand(command: C, snapshot: Snapshot<A>?): Result<StatefulSession<A, E>>
}

/**
 * To serialize events into plain JSON (integration events)
 */
interface EventSerializer<E : Any> {
  fun toJson(e: E): Result<JsonObject>
}

/**
 * To deserialize integration events from upstream services
 */
interface EventDeserializer<E : DomainEvent> {
  fun fromJson(type: String, j: JsonObject): Result<E>
}

// es/cqrs infra

/**
 * To perform aggregate root business methods and track it's events and state
 */
class StatefulSession<A : AggregateRoot, E : DomainEvent> {
  val originalVersion: Int
  private val originalState: A
  private val eventHandler: EventHandler<A, E>
  private val appliedEvents = mutableListOf<E>()
  var currentState: A

  constructor(version: Int, state: A, eventHandler: EventHandler<A, E>) {
    this.originalVersion = version
    this.originalState = state
    this.eventHandler = eventHandler
    this.currentState = originalState
  }

  constructor(constructorResult: CommandHandler.ConstructorResult<A, E>, eventHandler: EventHandler<A, E>) {
    this.originalVersion = 1
    this.originalState = constructorResult.state
    this.eventHandler = eventHandler
    this.currentState = originalState
    constructorResult.events.forEach {
      appliedEvents.add(it)
    }
  }

  fun appliedEvents(): List<E> {
    return appliedEvents
  }

  fun apply(events: List<E>): StatefulSession<A, E> {
    events.forEach { domainEvent ->
      currentState = eventHandler.handleEvent(currentState, domainEvent)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  inline fun execute(fn: (A) -> List<E>): StatefulSession<A, E> {
    val newEvents = fn.invoke(currentState)
    return apply(newEvents)
  }
}

/**
 * A Snapshot is an aggregate state with a version
 */
data class Snapshot<A : AggregateRoot>(val state: A, val version: Int)

/**
 * A metadata for the command. The REST/RPC controller should know how to instantiate it.
 */
data class CommandMetadata(
  val aggregateRootId: Int,
  val id: UUID = UUID.randomUUID(),
  val causationId: UUID = id,
  val correlationID: UUID = id
)

/**
 * An exception informing an concurrency violation
 */
class OptimisticConcurrencyConflict(message: String) : IllegalStateException(message)

/**
 * An event store to append new events
 */
interface EventStore<A : AggregateRoot, C : Command, E : DomainEvent> {
  fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void>
}

/**
 * A repository for snapshots
 */
interface SnapshotRepository<A : AggregateRoot, C : Command, E : DomainEvent> {
  fun get(id: Int): Future<Snapshot<A>?>
  fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void>
}
