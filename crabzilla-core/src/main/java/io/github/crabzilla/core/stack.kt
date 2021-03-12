package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.SharedData
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

// es/cqrs infra stack

/**
 * The REST/RPC controller must knows how to instantiate it.
 */
data class CommandMetadata(
  val aggregateRootId: Int,
  val id: UUID = UUID.randomUUID(),
  val causationId: UUID = id,
  val correlationID: UUID = id
)

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
 * An exception informing an concurrency violation
 */
class OptimisticConcurrencyConflict(message: String) : IllegalStateException(message)

/**
 * A simple SnapshotRepo in memory implementation
 */
class DefaultSnapshotRepo<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val sharedData: SharedData, // TODO how to avoid to get the map on every time?
  private val json: Json,
  private val entityName: String
) : SnapshotRepository<A, C, E> {

  companion object {
    internal val log = LoggerFactory.getLogger(DefaultSnapshotRepo::class.java)
  }

  override fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void> {
    val promise = Promise.promise<Void>()
    val stateAsJson = JsonObject(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, snapshot.state))
    val mapEntryAsJson = JsonObject().put("version", snapshot.version).put("state", stateAsJson)
    sharedData.getAsyncMap<Int, String>(entityName)
      .compose { map -> map.put(id, mapEntryAsJson.encode()) }
      .onFailure { promise.fail(it.cause) }
      .onSuccess { promise.complete() }
    return promise.future()
  }

  override fun get(id: Int): Future<Snapshot<A>?> {
    val promise = Promise.promise<Snapshot<A>>()
    sharedData.getAsyncMap<Int, String>(entityName)
      .compose { map -> map.get(id) }
      .onFailure { promise.fail(it.cause) }
      .onSuccess { snapshotJson ->
        if (snapshotJson == null) {
          promise.complete(null)
        } else {
          val mapEntryAsJson = JsonObject(snapshotJson)
          val version = mapEntryAsJson.getInteger("version")
          val stateAsJson = mapEntryAsJson.getJsonObject("state")
          val state = json.decodeFromString(AGGREGATE_ROOT_SERIALIZER, stateAsJson.encode()) as A
          promise.complete(Snapshot(state, version))
        }
      }
    return promise.future()
  }
}
