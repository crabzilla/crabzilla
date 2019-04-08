package io.github.crabzilla.pgclient

import io.github.crabzilla.*
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json

class PgClientSnapshotRepo<A : Entity>(private val pgPool: PgPool,
                                       private val seedValue: A,
                                       private val applyEventsFn: (DomainEvent, A) -> A,
                                       private val clazz: Class<A>) : SnapshotRepository<A> {
  companion object {

    const val SELECT_SNAPSHOT_VERSION = "select version, json_content from snapshot where ar_id = $1 and ar_name = $2"
    const val SELECT_EVENTS_VERSION_AFTER_VERSION = "select uow_events, version from units_of_work " +
      "where ar_id = $1 and ar_name = $2 and version > $3 order by version "

  }

  override fun retrieve(id: Int, entityName: String, aHandler: Handler<AsyncResult<Snapshot<A>>>) {

    val future = Future.future<Snapshot<A>>()
    future.setHandler(aHandler)

    pgPool.getConnection { res ->

      if (!res.succeeded()) {
        future.fail("when getting db connection")
        return@getConnection
      }

      // Transaction must use a connection
      val conn = res.result()

      // Begin the transaction
      val tx = conn.begin().abortHandler {
        println("Transaction failed")
      }

      val params = Tuple.of(id, entityName)

      // get current snapshot
      conn.preparedQuery(SELECT_SNAPSHOT_VERSION, params) { event1 ->

        if (event1.failed()) {
          tx.rollback()
          conn.close()
          future.fail(event1.cause())
          return@preparedQuery
        }

        val pgRow = event1.result()

        val cachedInstance : A
        val cachedVersion : Int

        if (pgRow == null || pgRow.size() == 0) {
          cachedInstance = seedValue
          cachedVersion = 0
        } else {
          cachedInstance = Json.decodeValue(pgRow.first().getJson("json_content").toString(), clazz)
          cachedVersion = pgRow.first().getInteger("version")
        }

        // get committed events after snapshot version
        conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION) { event2 ->

          if (!event2.succeeded()) {
            tx.rollback()
            conn.close()
            future.fail(event2.cause())
            return@prepare
          }

          var currentInstance = cachedInstance
          var currentVersion = cachedVersion

          val pq = event2.result()

          // Fetch 100 rows at a time
          val stream = pq.createStream(100, Tuple.of(id, entityName, cachedVersion))

          stream.exceptionHandler { err ->
            println("Error: ${err.message}")
            tx.rollback()
            conn.close()
            future.fail(err)
          }

          stream.endHandler {
            println("End of stream")
            // Attempt to commit the transaction
            tx.commit { ar ->
              // Return the connection to the pool
              conn.close()
              // But transaction abortion fails it
              if (ar.failed()) {
                future.fail(ar.cause())
              } else {
                val result = Snapshot(currentInstance, currentVersion)
                future.complete(result)
              }
            }
          }

          stream.handler { row ->
            currentVersion = row.getInteger(1)
            val events = listOfEventsFromJson(row.getJson(0).value().toString())
            currentInstance = events.fold(currentInstance) {state, event -> applyEventsFn(event, state)}
            println("Events: $events \n version: $currentVersion \n instance $currentInstance")
          }

        }

      }

    }

  }

}
