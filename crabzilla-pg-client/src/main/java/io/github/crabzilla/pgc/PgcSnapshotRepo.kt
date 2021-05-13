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
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID

class PgcSnapshotRepo<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val config: AggregateRootConfig<A, C, E>,
  private val writeModelDb: PgPool
) : SnapshotRepository<A, C, E> {

  companion object {
    private val log = LoggerFactory.getLogger(PgcSnapshotRepo::class.java)
  }

  override fun upsert(id: UUID, snapshot: Snapshot<A>): Future<Void> {

    fun upsertSnapshot(): String {
      return "INSERT INTO ${config.snapshotTableName.value} (ar_id, version, json_content) " +
        " VALUES ($1, $2, $3) " +
        " ON CONFLICT (ar_id) DO UPDATE SET version = $2, json_content = $3"
    }

    val json = JsonObject(config.json.encodeToString(AGGREGATE_ROOT_SERIALIZER, snapshot.state))
    val insertSql = upsertSnapshot()
    val tuple = Tuple.of(id, snapshot.version, json)
    return writeModelDb.preparedQuery(insertSql).execute(tuple).mapEmpty()
  }

  override fun get(id: UUID): Future<Snapshot<A>?> {

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

    val promise = Promise.promise<Snapshot<A>>()
    writeModelDb.connection
      .onFailure {
        log.error("When getting connection", it)
        promise.fail(it)
      }
      .onSuccess { conn: SqlConnection ->
        currentSnapshot(conn)
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
