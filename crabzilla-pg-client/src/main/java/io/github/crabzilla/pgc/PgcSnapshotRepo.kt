package io.github.crabzilla.pgc

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.pgc.PgcClient.close
import io.github.crabzilla.stack.SnapshotRepository
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.decodeFromString
import org.slf4j.LoggerFactory

class PgcSnapshotRepo<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val config: AggregateRootConfig<A, C, E>,
  private val writeModelDb: PgPool
) : SnapshotRepository<A, C, E> {

  companion object {
    private val log = LoggerFactory.getLogger(PgcSnapshotRepo::class.java)
//    private const val SELECT_EVENTS_VERSION_AFTER_VERSION =
//      "SELECT event_payload, version " +
//        "FROM events " +
//        "WHERE ar_id = $1 " +
//        "AND ar_name = $2 " +
//        "AND version > $3 " +
//        "ORDER BY event_id "
    private const val ROWS_PER_TIME = 1000
  }

  override fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void> {

    fun upsertSnapshot(): String {
      return "INSERT INTO ${config.snapshotTableName.value} (ar_id, version, json_content) " +
        " VALUES ($1, $2, $3) " +
        " ON CONFLICT (ar_id) DO UPDATE SET version = $2, json_content = $3"
    }

    val promise = Promise.promise<Void>()
    val json = JsonObject(config.json.encodeToString(AGGREGATE_ROOT_SERIALIZER, snapshot.state))
    val insertSql = upsertSnapshot()
    val tuple = Tuple.of(id, snapshot.version, json)
    writeModelDb.preparedQuery(insertSql)
      .execute(tuple) { insert ->
        if (insert.failed()) {
          log.error("upsert snapshot query error", insert.cause())
          promise.fail(insert.cause())
        } else {
          if (log.isDebugEnabled) log.debug("upsert snapshot success $id $json")
          promise.complete()
        }
      }
    return promise.future()
  }

  override fun get(id: Int): Future<Snapshot<A>?> {

    fun selectSnapshot(): String {
      return "SELECT version, json_content FROM ${config.snapshotTableName.value} WHERE ar_id = $1"
    }

    fun currentSnapshot(conn: SqlConnection): Future<Pair<SqlConnection, Snapshot<A>?>> {
      val promise = Promise.promise<Pair<SqlConnection, Snapshot<A>?>>()
      fun snapshot(rowSet: RowSet<Row>): Snapshot<A>? {
        return if (rowSet.size() == 0) {
          null
        } else {
          val stateAsJson: JsonObject = rowSet.first().get(JsonObject::class.java, 1)
          val state = config.json.decodeFromString(AGGREGATE_ROOT_SERIALIZER, stateAsJson.encode()) as A
          Snapshot(state, rowSet.first().getInteger("version"))
        }
      }
      conn.preparedQuery(selectSnapshot())
        .execute(Tuple.of(id))
        .onSuccess { pgRow -> promise.complete(Pair(conn, snapshot(pgRow))) }
        .onFailure {
          log.error(it.message)
          promise.complete(Pair(conn, null))
        }
      return promise.future()
    }

//    fun newSnapshot(conn: SqlConnection, snapshot: Snapshot<A>?): Future<Snapshot<A>?> {
//      val promise = Promise.promise<Snapshot<A>>()
//      val events = mutableListOf<E>()
//      var currentVersion = 0
//      conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION) { ar0 ->
//        if (ar0.failed()) {
//          promise.fail(ar0.cause())
//          return@prepare
//        }
//        val pq: PreparedStatement = ar0.result()
//        // Streams require to run within a transaction
//        conn.begin { ar1 ->
//          if (ar1.failed()) {
//            log.error("When starting transaction", ar1.cause())
//            promise.fail(ar1.cause())
//          } else {
//            val tx: Transaction = ar1.result()
//            // Fetch ROWS_PER_TIME
//            val stream =
//              pq.createStream(ROWS_PER_TIME, Tuple.of(id, config.name.value, snapshot?.version ?: 0))
//            // Use the stream
//            stream.exceptionHandler { err -> log.error("Stream error", err) }
//            stream.handler { row ->
//              val jsonObject: JsonObject = row.get(JsonObject::class.java, 0)
//              val event: DomainEvent = config.json.decodeFromString(jsonObject.encode())
//              currentVersion = row.getInteger(1)
//              events.add(event as E)
//              if (log.isDebugEnabled) log.debug("Event: $event version: $currentVersion")
//            }
//            stream.endHandler {
//              if (log.isDebugEnabled) log.debug("End of stream")
//              // Attempt to commit the transaction
//              tx.commit { ar ->
//                if (ar.failed()) {
//                  log.error("tx.commit", ar.cause())
//                  promise.fail(ar.cause())
//                } else {
//                  if (log.isDebugEnabled) log.debug("tx.commit successfully")
//                  if (events.size == 0) {
//                    promise.complete(snapshot)
//                  } else {
//                    val currentInstance = events.fold(
//                      snapshot?.state,
//                      { state, event -> config.eventHandler.handleEvent(state, event) }
//                    )
//                    promise.complete(Snapshot(currentInstance!!, currentVersion))
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//      return promise.future()
//    }

    val promise = Promise.promise<Snapshot<A>>()
    writeModelDb.connection
      .onFailure {
        log.error("When getting connection", it)
        promise.fail(it)
      }
      .onSuccess { conn: SqlConnection ->
        currentSnapshot(conn)
  //        .compose { pair -> newSnapshot(pair.first, pair.second) }
          .onSuccess { newSnapshot -> promise.complete(newSnapshot.second) }
          .onFailure {
            log.error("When getting new snapshot", it)
            promise.fail(it)
          }
          .onComplete {
            close(conn)
          }
      }
    return promise.future()
  }
}
