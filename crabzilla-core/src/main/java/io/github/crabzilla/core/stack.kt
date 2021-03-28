package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.SharedData
import io.vertx.kotlin.core.json.jsonObjectOf
import kotlinx.serialization.json.Json

// es/cqrs infra stack

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
 * An event record
 */
data class EventRecord(val aggregateName: String, val aggregateId: Int, val eventAsjJson: JsonObject, val eventId: Long) {
  companion object {
    fun fromJsonObject(asJsonObject: JsonObject): EventRecord {
      return EventRecord(
        asJsonObject.getString("aggregateName"),
        asJsonObject.getInteger("aggregateId"),
        asJsonObject.getJsonObject("eventAsjJson"),
        asJsonObject.getLong("eventId")
      )
    }
  }
  fun toJsonObject(): JsonObject {
    return jsonObjectOf(
      Pair("aggregateName", aggregateName),
      Pair("aggregateId", aggregateId),
      Pair("eventAsjJson", eventAsjJson),
      Pair("eventId", eventId)
    )
  }
}

/**
 * To publish an event as JSON to read model, messaging broker, etc (any side effect)
 */
interface EventsPublisher {
  fun publish(eventRecords: List<EventRecord>): Future<Long>
  // what about correlation id, etc?
}

/**
 * An exception informing an concurrency violation
 */
class OptimisticConcurrencyConflict(message: String) : IllegalStateException(message)

/**
 * A simple SnapshotRepo in memory implementation
 */
class DefaultSnapshotRepo<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val sharedData: SharedData,
  private val json: Json,
  private val entityName: String
) : SnapshotRepository<A, C, E> {

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
