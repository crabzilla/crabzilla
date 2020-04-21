package io.github.crabzilla.pgc

import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.ENTITY_SERIALIZER
import io.github.crabzilla.framework.EVENT_SERIALIZER
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.EntityCommandAware
import io.github.crabzilla.framework.Snapshot
import io.github.crabzilla.internal.SnapshotRepository
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.logging.LoggerFactory
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

class PgcSnapshotRepo<E : Entity>(
  private val writeModelDb: PgPool,
  private val json: Json,
  private val entityName: String,
  private val entityFn: EntityCommandAware<E>
) : SnapshotRepository<E> {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcSnapshotRepo::class.java)
    const val SELECT_EVENTS_VERSION_AFTER_VERSION = "SELECT uow_events, version FROM units_of_work " +
      "WHERE ar_id = $1 and ar_name = $2 and version > $3 ORDER BY version "
    private val ROWS_PER_TIME = 1000
  }
  private fun selectSnapshot(): String {
    return "SELECT version, json_content FROM ${entityName}_snapshots WHERE ar_id = $1"
  }
  private fun upsertSnapshot(): String {
    return "INSERT INTO ${entityName}_snapshots (ar_id, version, json_content) " +
      " VALUES ($1, $2, $3) " +
      " ON CONFLICT (ar_id) DO UPDATE SET version = $2, json_content = $3"
  }
  override fun upsert(entityId: Int, snapshot: Snapshot<E>): Future<Void> {
    val promise = Promise.promise<Void>()
    val json: String = json.stringify(ENTITY_SERIALIZER, snapshot.state)
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
    writeModelDb.getConnection { res ->
      if (!res.succeeded()) {
        promise.fail(res.cause())
        return@getConnection
      } else {
        // Transaction must use a connection
        val conn = res.result()
        // TODO how to specify transaction isolation level?
        // Begin the transaction
        val tx: Transaction = conn.begin().abortHandler {
            log.error("Transaction aborted")
            promise.fail("Transaction aborted")
            return@abortHandler
        }
        // get current snapshot
        conn.preparedQuery(selectSnapshot())
          .execute(Tuple.of(entityId)) { event1 ->
          if (event1.failed()) {
            conn.close()
            promise.fail(event1.cause())
            return@execute
          } else {
            val pgRow = event1.result()
            val cachedInstance: E
            val cachedVersion: Int
            if (pgRow == null || pgRow.size() == 0) {
              cachedInstance = entityFn.initialState
              cachedVersion = 0
            } else {
              val stateAsJson: String = pgRow.first().get(String::class.java, 1)
              cachedInstance = json.parse(ENTITY_SERIALIZER, stateAsJson) as E
              cachedVersion = pgRow.first().getInteger("version")
            }
            // get committed events after snapshot version
            conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION) { event2 ->
              if (!event2.succeeded()) {
                conn.close()
                promise.fail(event2.cause())
                return@prepare
              } else {
                var currentInstance = cachedInstance
                var currentVersion = cachedVersion
                val pq = event2.result()
                // Fetch N rows at a time
                val stream = pq.createStream(ROWS_PER_TIME, Tuple.of(entityId, entityName, cachedVersion))
                stream.exceptionHandler { err -> log.error("Retrieve: ${err.message}", err)
                  tx.rollback(); conn.close(); promise.fail(err)
                }
                stream.endHandler {
                  log.debug("End of stream")
                  // Attempt to commit the transaction
                  tx.commit { ar ->
                    // Return the connection to the pool
                    conn.close()
                    // But transaction abortion fails it
                    if (ar.failed()) {
                      log.error("endHandler.closeConnection")
                      promise.fail(ar.cause())
                    } else {
                      log.debug("success: endHandler.closeConnection")
                      val result = Snapshot(currentInstance, currentVersion)
                      promise.complete(result)
                    }
                  }
                }
                stream.handler { row ->
                  currentVersion = row.getInteger(1)
                  val eventsJsonString: String = row.get(String::class.java, 0)
                  val events: List<DomainEvent> = json.parse(EVENT_SERIALIZER.list, eventsJsonString)
                  currentInstance = events.fold(currentInstance) { state, event -> entityFn.applyEvent.invoke(event, state) }
                  log.debug("Events: $events \n version: $currentVersion \n instance $currentInstance")
                }
              }
            }
          }
        }
      }
    }
    return promise.future()
  }
}
