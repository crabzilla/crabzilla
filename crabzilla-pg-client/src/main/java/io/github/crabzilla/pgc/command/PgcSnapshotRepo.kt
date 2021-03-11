// package io.github.crabzilla.pgc.command
//
// import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
// import io.github.crabzilla.core.AggregateRoot
// import io.github.crabzilla.core.AggregateRootCommandAware
// import io.github.crabzilla.core.DomainEvent
// import io.github.crabzilla.core.Snapshot
// import io.github.crabzilla.core.SnapshotRepository
// import io.vertx.core.Future
// import io.vertx.core.Promise
// import io.vertx.core.json.JsonObject
// import io.vertx.pgclient.PgPool
// import io.vertx.sqlclient.PreparedStatement
// import io.vertx.sqlclient.Row
// import io.vertx.sqlclient.RowSet
// import io.vertx.sqlclient.SqlConnection
// import io.vertx.sqlclient.Transaction
// import io.vertx.sqlclient.Tuple
// import kotlinx.serialization.decodeFromString
// import kotlinx.serialization.json.Json
// import org.slf4j.LoggerFactory
//
// class PgcSnapshotRepo<A : AggregateRoot>(
//  private val writeModelDb: PgPool,
//  private val json: Json,
//  private val commandAware: AggregateRootCommandAware<A>
// ) : SnapshotRepository<A> {
//
//  companion object {
//    private val log = LoggerFactory.getLogger(PgcSnapshotRepo::class.java)
//    private const val SELECT_EVENTS_VERSION_AFTER_VERSION = "SELECT event_payload, version FROM crabz_events " +
//      "WHERE ar_id = $1 and ar_name = $2 and version > $3 ORDER BY version "
//    private const val ROWS_PER_TIME = 1000
//  }
//
//  override fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void> {
//    fun upsertSnapshot(): String {
//      return "INSERT INTO crabz_${commandAware.entityName}_snapshots (ar_id, version, json_content) " +
//        " VALUES ($1, $2, $3) " +
//        " ON CONFLICT (ar_id) DO UPDATE SET version = $2, json_content = $3"
//    }
//    val promise = Promise.promise<Void>()
//    val json = JsonObject(json.encodeToString(AGGREGATE_ROOT_SERIALIZER, snapshot.state))
//    writeModelDb.preparedQuery(upsertSnapshot())
//      .execute(Tuple.of(id, snapshot.version, json)) { insert ->
//      if (insert.failed()) {
//        log.error("upsert snapshot query error")
//        promise.fail(insert.cause())
//      } else {
//        log.debug("upsert snapshot success")
//        promise.complete()
//      }
//    }
//    return promise.future()
//  }
//
//  override fun retrieve(id: Int): Future<Snapshot<A>> {
//    fun selectSnapshot(): String {
//      return "SELECT version, json_content FROM crabz_${commandAware.entityName}_snapshots WHERE ar_id = $1"
//    }
//    fun cached(conn: SqlConnection): Future<Pair<A, Int>> {
//      val promise = Promise.promise<Pair<A, Int>>()
//      fun pair(rowSet: RowSet<Row>): Pair<A, Int> {
//        return if (rowSet.size() == 0) {
//          Pair(commandAware.initialState, 0)
//        } else {
//          val stateAsJson: JsonObject = rowSet.first().get(JsonObject::class.java, 1)
//          val state = json.decodeFromString(AGGREGATE_ROOT_SERIALIZER, stateAsJson.encode()) as A
//          Pair(state, rowSet.first().getInteger("version"))
//        }
//      }
//      conn.preparedQuery(selectSnapshot())
//        .execute(Tuple.of(id))
//        .onSuccess { pgRow -> promise.complete(pair(pgRow)) }
//        .onFailure {
//          log.error(it.message)
//          promise.complete(Pair(commandAware.initialState, 0))
//        }
//      return promise.future()
//    }
//    fun buildSnapshot(conn: SqlConnection, cachedVersion: Pair<A, Int>): Future<Snapshot<A>>? {
//      val promise = Promise.promise<Snapshot<A>>()
//      var currentInstance = cachedVersion.first
//      var currentVersion = cachedVersion.second
//      conn.prepare(SELECT_EVENTS_VERSION_AFTER_VERSION) { ar0 ->
//        if (ar0.succeeded()) {
//          val pq: PreparedStatement = ar0.result()
//          // Streams require to run within a transaction
//          conn.begin { ar1 ->
//            if (ar1.succeeded()) {
//              val tx: Transaction = ar1.result()
//              // Fetch ROWS_PER_TIME
//              val stream = pq.createStream(ROWS_PER_TIME, Tuple.of(id, commandAware.entityName, cachedVersion.second))
//              // Use the stream
//              stream.exceptionHandler { err -> log.error("Stream error", err) }
//              stream.handler { row ->
//                val jsonObject: JsonObject = row.get(JsonObject::class.java, 0)
//                val event: DomainEvent = json.decodeFromString(jsonObject.encode())
//                currentVersion = row.getInteger(1)
//                currentInstance.apply(event)
//                if (log.isDebugEnabled) {
//                  log.debug("Event: $event \n version: $currentVersion \n instance $currentInstance")
//                }
//              }
//              stream.endHandler {
//                if (log.isDebugEnabled) log.debug("End of stream")
//                // Attempt to commit the transaction
//                tx.commit { ar ->
//                  if (ar.failed()) {
//                    log.error("tx.commit", ar.cause())
//                    promise.fail(ar.cause())
//                  } else {
//                    if (log.isDebugEnabled) log.debug("tx.commit successfully")
//                    val result = Snapshot(currentInstance, currentVersion)
//                    promise.complete(result)
//                  }
//                }
//              }
//            }
//          }
//        }
//      }
//      return promise.future()
//    }
//
//    val promise = Promise.promise<Snapshot<A>>()
//    writeModelDb.connection // Transaction must use a connection
//      .onSuccess { conn: SqlConnection -> cached(conn)
//        .compose { cached -> buildSnapshot(conn, cached) }
//        .onSuccess { snapshot -> promise.complete(snapshot) }
//        .onFailure { err -> promise.fail(err) }
//      }
//      .onFailure { err -> promise.fail(err) }
//    return promise.future()
//  }
// }
