package io.github.crabzilla.postgres.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.json.JsonSerDer
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID

class PersistentSnapshotRepo<S : State, E : Event>(
  private val stateName: String,
  private val jsonSerDer: JsonSerDer
) :
  SnapshotRepository<S, E> {

  companion object {
    private val log = LoggerFactory.getLogger(PersistentSnapshotRepo::class.java)
    private const val SQL_GET_SNAPSHOT =
      """ SELECT version, json_content
          FROM snapshots s
         WHERE ar_id = $1 
           AND ar_type = $2"""
    private const val SQL_INSERT_VERSION =
      """ INSERT INTO snapshots (version, json_content, ar_id, ar_type)
          VALUES ($1, $2, $3, $4)"""
    private const val SQL_UPDATE_VERSION =
      """ UPDATE snapshots 
          SET version = $1, json_content = $2 
          WHERE ar_id = $3 
           AND version = $4
           AND ar_type = $5"""
  }

  override fun get(
    conn: SqlConnection,
    id: UUID
  ): Future<Snapshot<S>?> {
    fun snapshot(rowSet: RowSet<Row>): Snapshot<S>? {
      return if (rowSet.size() == 0) {
        null
      } else {
        val row = rowSet.first()
        val version = row.getInteger("version")
        val stateAsJson = JsonObject(row.getValue("json_content").toString())
        val state = jsonSerDer.stateFromJson(stateAsJson.toString()) as S
        Snapshot(state, version)
      }
    }
    return conn
      .preparedQuery(SQL_GET_SNAPSHOT)
      .execute(Tuple.of(id, stateName))
      .map { pgRow -> snapshot(pgRow) }
  }

  override fun upsert(
    conn: SqlConnection,
    id: UUID,
    originalVersion: Int,
    resultingVersion: Int,
    newState: S
  ): Future<Void> {
    val newSTateAsJson = jsonSerDer.toJson(newState)
    log.debug("Will append {} snapshot {} to version {}", id, newSTateAsJson, resultingVersion)
    return if (originalVersion == 0) {
      val params = Tuple.of(
        resultingVersion,
        JsonObject(newSTateAsJson),
        id,
        stateName
      )
      conn.preparedQuery(SQL_INSERT_VERSION)
        .execute(params)
        .mapEmpty()
    } else {
      val params = Tuple.of(
        resultingVersion,
        JsonObject(newSTateAsJson),
        id,
        originalVersion,
        stateName
      )
      conn
        .preparedQuery(SQL_UPDATE_VERSION)
        .execute(params)
        .mapEmpty()
    }
  }
}
