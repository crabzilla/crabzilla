package io.github.crabzilla.pgc

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.Entity
import io.github.crabzilla.Snapshot
import io.github.crabzilla.SnapshotRepository
import io.github.crabzilla.UnitOfWork.JsonMetadata.EVENTS_JSON_CONTENT
import io.github.crabzilla.UnitOfWork.JsonMetadata.EVENT_NAME
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgTransaction
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory

class PgcSnapshotRepo<E : Entity>(private val entityName: String,
                                  private val pgPool: PgPool,
                                  private val seedValue: E,
                                  private val applyEventsFn: (DomainEvent, E) -> E,
                                  private val writeModelFromJson: (JsonObject) -> E,
                                  private val writeModelToJson: (E) -> JsonObject,
                                  private val eventFromJson: (String, JsonObject) -> DomainEvent)
                                                                          : SnapshotRepository<E> {


  companion object {

    internal val log = LoggerFactory.getLogger(PgcSnapshotRepo::class.java)
    const val SELECT_EVENTS_VERSION_AFTER_VERSION = "SELECT uow_events, version FROM units_of_work " +
      "WHERE ar_id = $1 and ar_name = $2 and version > $3 ORDER BY version "

  }


  private fun selectSnapshot(): String {
    return "SELECT version, json_content FROM ${entityName}_snapshots WHERE ar_id = $1"
  }

  private fun upsertSnapshot(): String {
    return "INSERT INTO ${entityName}_snapshots (ar_id, version, json_content) " +
      " VALUES ($1, $2, $3) " +
      " ON CONFLICT (ar_id) DO UPDATE SET version = $2, json_content = $3"
  }

  override fun upsert(entityId: Int, snapshot: Snapshot<E>, aHandler: Handler<AsyncResult<Void>>) {

    val json = io.reactiverse.pgclient.data.Json.create(writeModelToJson.invoke(snapshot.state))

    pgPool.preparedQuery(upsertSnapshot(), Tuple.of(entityId, snapshot.version, json)) { insert ->
      if (insert.failed()) {
        log.error("upsert snapshot query error")
        aHandler.handle(Future.failedFuture(insert.cause()))
      } else {
        log.trace("upsert snapshot success")
        aHandler.handle(Future.succeededFuture())
      }
    }

  }

  override fun retrieve(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>) {

    val future = Future.future<Snapshot<E>>()
    future.setHandler(aHandler)

    pgPool.getConnection { res ->

      if (!res.succeeded()) {
        future.fail("retrieve.getConnection");
        return@getConnection

      } else {

        // Transaction must use a connection
        val conn = res.result()

        // TODO how to specify transaction isolation level?
        // Begin the transaction
        val tx: PgTransaction = conn.begin().abortHandler { log.error("Transaction failed") }

        // get current snapshot
        conn.preparedQuery(selectSnapshot(), Tuple.of(entityId)) { event1 ->

          if (event1.failed()) {
            tx.rollback(); conn.close(); future.fail(event1.cause());
            return@preparedQuery

          } else {
            val pgRow = event1.result()

            val cachedInstance : E
            val cachedVersion : Int

            if (pgRow == null || pgRow.size() == 0) {
              cachedInstance = seedValue
              cachedVersion = 0
            } else {
              cachedInstance = writeModelFromJson.invoke(JsonObject(pgRow.first().getJson("json_content").toString()))
              cachedVersion = pgRow.first().getInteger("version")
            }

            // get committed events after snapshot version
            conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION) { event2 ->

              if (!event2.succeeded()) {
                tx.rollback(); conn.close(); future.fail(event2.cause()); return@prepare

              } else {
                var currentInstance = cachedInstance
                var currentVersion = cachedVersion

                val pq = event2.result()

                // Fetch 100 rows at a time
                val stream = pq.createStream(100, Tuple.of(entityId, entityName, cachedVersion))

                stream.exceptionHandler { err -> log.error("Retrieve: ${err.message}", err)
                  tx.rollback(); conn.close(); future.fail(err)
                }

                stream.endHandler {
                  log.trace("End of stream")
                  // Attempt to commit the transaction
                  tx.commit { ar ->
                    // Return the connection to the pool
                    conn.close()
                    // But transaction abortion fails it
                    if (ar.failed()) {
                      log.error("endHandler.closeConnection")
                      future.fail(ar.cause())
                    } else {
                      log.trace("success: endHandler.closeConnection")
                      val result = Snapshot(currentInstance, currentVersion)
                      future.complete(result)
                    }
                  }
                }

                stream.handler { row ->
                  currentVersion = row.getInteger(1)
                  val eventsArray = JsonArray(row.getJson(0).value().toString())
                  val jsonToEvent: (Int) -> DomainEvent = { index ->
                    val jsonObject = eventsArray.getJsonObject(index)
                    val eventName = jsonObject.getString(EVENT_NAME)
                    val eventJson = jsonObject.getJsonObject(EVENTS_JSON_CONTENT)
                    eventFromJson.invoke(eventName, eventJson)
                  }

                  val events: List<DomainEvent> = List(eventsArray.size(), jsonToEvent)
                  currentInstance = events.fold(currentInstance) {state, event -> applyEventsFn(event, state)}
                  log.trace("Events: $events \n version: $currentVersion \n instance $currentInstance")

                }
              }
            }
          }
        }
      }
    }
  }
}
