package io.github.crabzilla.internal

import io.github.crabzilla.core.ENTITY_SERIALIZER
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.Snapshot
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.SharedData
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * An unused implementation to consider
 */
class InMemorySnapshotRepo<E : Entity>(
  private val sharedData: SharedData,
  private val json: Json,
  private val entityName: String,
  private val initialState: E
) : SnapshotRepository<E> {
  companion object {
    internal val log = LoggerFactory.getLogger(CommandController::class.java)
  }
  override fun upsert(entityId: Int, snapshot: Snapshot<E>): Future<Void> {
    val promise = Promise.promise<Void>()
    sharedData.getAsyncMap<Int, String>(entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed to get map $entityName")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      val stateAsJson = JsonObject(json.stringify(ENTITY_SERIALIZER, snapshot.state))
      val mapEntryAsJson = JsonObject().put("version", snapshot.version).put("state", stateAsJson)
      event1.result().put(entityId, mapEntryAsJson.encode()) { event2 ->
        if (event2.failed()) {
          log.error("Failed to put $entityId on map $entityName")
          promise.fail(event2.cause())
          return@put
        }
        promise.complete()
      }
    }
    return promise.future()
  }

  override fun retrieve(entityId: Int): Future<Snapshot<E>> {
    val promise = Promise.promise<Snapshot<E>>()
    val defaultSnapshot = Snapshot(initialState, 0)
    sharedData.getAsyncMap<Int, String>(entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed get map $entityName")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      event1.result().get(entityId) { event2 ->
        if (event2.failed()) {
          log.error("Failed to get $entityId on map $entityName")
          promise.fail(event2.cause())
          return@get
        }
        val result = event2.result()
        if (result == null) {
          if (log.isDebugEnabled) {
            log.debug("Returning default snapshot for $entityId on map $entityName")
          }
          promise.complete(defaultSnapshot)
          return@get
        }
        val mapEntryAsJson = JsonObject(event2.result())
        val version = mapEntryAsJson.getInteger("version")
        val stateAsJson = mapEntryAsJson.getJsonObject("state")
        val state = json.parse(ENTITY_SERIALIZER, stateAsJson.encode()) as E
        promise.complete(Snapshot(state, version))
      }
    }
    return promise.future()
  }
}
