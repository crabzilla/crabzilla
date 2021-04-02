package io.github.crabzilla.stack

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Snapshot
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.SharedData
import kotlinx.serialization.json.Json

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
