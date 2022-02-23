package io.github.crabzilla.pgclient.command.internal

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

internal class PersistentSnapshotRepo<S : Any, E : Any>(
  private val stateSerDer: PolymorphicSerializer<S>,
  private val json: Json) :
  SnapshotRepository<S, E> {

  companion object {
    private val log = LoggerFactory.getLogger(PersistentSnapshotRepo::class.java)
    private const val SQL_GET_SNAPSHOT =
      """ SELECT version, json_content
          FROM snapshots s
         WHERE state_id = $1 
           AND state_type = $2"""
    private const val SQL_INSERT_VERSION =
      """ INSERT INTO snapshots (version, json_content, state_id, state_type)
          VALUES ($1, $2, $3, $4)"""
    private const val SQL_UPDATE_VERSION =
      """ UPDATE snapshots 
          SET version = $1, json_content = $2 
          WHERE state_id = $3 
           AND version = $4
           AND state_type = $5"""
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
        val state = json.decodeFromString(stateSerDer, stateAsJson.toString())
        Snapshot(state, version)
      }
    }
    return conn
      .preparedQuery(SQL_GET_SNAPSHOT)
      .execute(Tuple.of(id, stateSerDer.descriptor.serialName))
      .map { pgRow -> snapshot(pgRow) }
  }

  override fun upsert(
    conn: SqlConnection,
    id: UUID,
    originalVersion: Int,
    resultingVersion: Int,
    newState: S
  ): Future<Void> {
    val newSTateAsJson = json.encodeToString(stateSerDer, newState)
    log.debug("Will append {} snapshot {} to version {}", id, newSTateAsJson, resultingVersion)
    return if (originalVersion == 0) {
      val params = Tuple.of(
        resultingVersion,
        JsonObject(newSTateAsJson),
        id,
        stateSerDer.descriptor.serialName
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
        stateSerDer.descriptor.serialName
      )
      conn
        .preparedQuery(SQL_UPDATE_VERSION)
        .execute(params)
        .mapEmpty()
    }
  }
}
