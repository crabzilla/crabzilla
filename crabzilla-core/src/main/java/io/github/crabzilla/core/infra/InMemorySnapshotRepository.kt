package io.github.crabzilla.core.infra

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootCommandAware
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.SnapshotRepository
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.SharedData
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class InMemorySnapshotRepository<A : AggregateRoot>(
  private val sharedData: SharedData, // TODO how to avoid to get the map on every time?
  private val json: Json,
  private val commandAware: AggregateRootCommandAware<A>
) : SnapshotRepository<A> {

  companion object {
    internal val log = LoggerFactory.getLogger(InMemorySnapshotRepository::class.java)
  }

  override fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void> {
    val promise = Promise.promise<Void>()
    sharedData.getAsyncMap<Int, String>(commandAware.entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed to get map ${commandAware.entityName}")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      val stateAsJson = JsonObject(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, snapshot.state))
      val mapEntryAsJson = JsonObject().put("version", snapshot.version).put("state", stateAsJson)
      event1.result().put(id, mapEntryAsJson.encode()) { event2 ->
        if (event2.failed()) {
          log.error("Failed to put $id on map ${commandAware.entityName}")
          promise.fail(event2.cause())
          return@put
        }
        promise.complete()
      }
    }
    return promise.future()
  }

  override fun retrieve(id: Int): Future<Snapshot<A>> {
    val promise = Promise.promise<Snapshot<A>>()
    val defaultSnapshot = Snapshot(commandAware.initialState, 0)
    sharedData.getAsyncMap<Int, String>(commandAware.entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed get map ${commandAware.entityName}")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      event1.result().get(id) { event2 ->
        if (event2.failed()) {
          log.error("Failed to get $id on map ${commandAware.entityName}")
          promise.fail(event2.cause())
          return@get
        }
        val result = event2.result()
        if (result == null) {
          if (log.isDebugEnabled) {
            log.debug("Returning default snapshot for $id on map ${commandAware.entityName}")
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
