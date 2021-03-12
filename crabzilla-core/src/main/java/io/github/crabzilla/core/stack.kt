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
    sharedData.getAsyncMap<Int, String>(entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed to get map $entityName")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      val stateAsJson = JsonObject(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, snapshot.state))
      val mapEntryAsJson = JsonObject().put("version", snapshot.version).put("state", stateAsJson)
      event1.result().put(id, mapEntryAsJson.encode()) { event2 ->
        if (event2.failed()) {
          log.error("Failed to put $id on map $entityName")
          promise.fail(event2.cause())
          return@put
        }
        promise.complete()
      }
    }
    return promise.future()
  }

  override fun get(id: Int): Future<Snapshot<A>?> {
    val promise = Promise.promise<Snapshot<A>>()
    val defaultSnapshot = null
    sharedData.getAsyncMap<Int, String>(entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed get map $entityName")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      event1.result().get(id) { event2 ->
        if (event2.failed()) {
          log.error("Failed to get $id on map $entityName")
          promise.fail(event2.cause())
          return@get
        }
        val result = event2.result()
        if (result == null) {
          if (log.isDebugEnabled) {
            log.debug("Returning default snapshot for $id on map $entityName")
          }
          promise.complete(defaultSnapshot)
          return@get
        }
        val mapEntryAsJson = JsonObject(event2.result())
        val version = mapEntryAsJson.getInteger("version")
        val stateAsJson = mapEntryAsJson.getJsonObject("state")
        val state = json.decodeFromString(AGGREGATE_ROOT_SERIALIZER, stateAsJson.encode()) as A
        promise.complete(Snapshot(state, version))
      }
    }
    return promise.future()
  }
}
