package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.command.DomainEvent
import io.github.crabzilla.core.command.ENTITY_SERIALIZER
import io.github.crabzilla.core.command.EVENT_SERIALIZER
import io.github.crabzilla.core.command.Entity
import io.github.crabzilla.core.command.EntityCommandAware
import io.github.crabzilla.core.command.Snapshot
import io.github.crabzilla.core.command.SnapshotRepository
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

class PgcSnapshotRepo<E : Entity>(
  private val writeModelDb: PgPool,
  private val json: Json,
  private val commandAware: EntityCommandAware<E>
) : SnapshotRepository<E> {

  companion object {
    private val log = LoggerFactory.getLogger(PgcSnapshotRepo::class.java)
    private const val SELECT_EVENTS_VERSION_AFTER_VERSION = "SELECT uow_events, version FROM units_of_work " +
      "WHERE ar_id = $1 and ar_name = $2 and version > $3 ORDER BY version "
    private const val ROWS_PER_TIME = 1000
  }
  private fun selectSnapshot(): String {
    return "SELECT version, json_content FROM ${commandAware.entityName}_snapshots WHERE ar_id = $1"
  }
  private fun upsertSnapshot(): String {
    return "INSERT INTO ${commandAware.entityName}_snapshots (ar_id, version, json_content) " +
      " VALUES ($1, $2, $3) " +
      " ON CONFLICT (ar_id) DO UPDATE SET version = $2, json_content = $3"
  }
  override fun upsert(entityId: Int, snapshot: Snapshot<E>): Future<Void> {
    val promise = Promise.promise<Void>()
    val json = JsonObject(json.stringify(ENTITY_SERIALIZER, snapshot.state))
    writeModelDb.preparedQuery(upsertSnapshot())
      .execute(Tuple.of(entityId, snapshot.version, json)) { insert ->
      if (insert.failed()) {
        log.error("upsert snapshot query error")
        promise.fail(insert.cause())
      } else {
        log.debug("upsert snapshot success")
        promise.complete()
      }
    }
    return promise.future()
  }

  override fun retrieve(entityId: Int): Future<Snapshot<E>> {
    val promise = Promise.promise<Snapshot<E>>()
    writeModelDb.begin { event0 ->
      if (event0.failed()) {
        log.error("when starting transaction", event0.cause())
        promise.fail(event0.cause())
        return@begin
      }
      val tx = event0.result()
      // get current snapshot
      tx.preparedQuery(selectSnapshot())
        .execute(Tuple.of(entityId)) { event1 ->
          if (event1.failed()) {
            promise.fail(event1.cause())
            return@execute
          }
          val pgRow = event1.result()
          val cachedInstance: E
          val cachedVersion: Int
          if (pgRow == null || pgRow.size() == 0) {
            cachedInstance = commandAware.initialState
            cachedVersion = 0
          } else {
            val stateAsJson: JsonObject = pgRow.first().get(JsonObject::class.java, 1)
            cachedInstance = json.parse(ENTITY_SERIALIZER, stateAsJson.encode()) as E
            cachedVersion = pgRow.first().getInteger("version")
          }
          // get committed events after snapshot version
          tx.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION) { event2 ->
            if (event2.failed()) {
              log.error("when gettting committed events after snapshot version", event2.cause())
              promise.fail(event2.cause())
              return@prepare
            }
            var currentInstance = cachedInstance
            var currentVersion = cachedVersion
            val pq = event2.result()
            // Fetch N rows at a time
            val stream = pq.createStream(ROWS_PER_TIME, Tuple.of(entityId, commandAware.entityName, cachedVersion))
            stream.exceptionHandler { err -> log.error("Stream error", err) }
            stream.handler { row ->
              currentVersion = row.getInteger(1)
              val eventsJsonString: String = row.get(String::class.java, 0)
              val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsJsonString)
              currentInstance =
                events.fold(currentInstance) { state, event -> commandAware.applyEvent.invoke(event, state) }
              if (log.isDebugEnabled) {
                log.debug("Events: $events \n version: $currentVersion \n instance $currentInstance")
              }
            }
            stream.endHandler {
              if (log.isDebugEnabled) log.debug("End of stream")
              // Attempt to commit the transaction
              tx.commit { ar ->
                if (ar.failed()) {
                  log.error("tx.commit", ar.cause())
                  promise.fail(ar.cause())
                } else {
                  if (log.isDebugEnabled) log.debug("tx.commit successfully")
                  val result = Snapshot(currentInstance, currentVersion)
                  promise.complete(result)
                }
              }
            }
          }
        }
    }
    return promise.future()
  }
}
