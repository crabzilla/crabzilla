package io.github.crabzilla.pgc

import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.Snapshot
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

class SnapshotRepository<A : State>(
  private val sqlClient: SqlClient,
  private val json: Json
) {

  companion object {
    private val log = LoggerFactory.getLogger(SnapshotRepository::class.java)
    const val SQL_SELECT_VERSION =
      """ SELECT version, json_content
          FROM SNAPSHOTS 
          WHERE ar_id = $1"""
  }

  fun get(id: UUID): Future<Snapshot<A>?> {
    fun snapshot(rowSet: RowSet<Row>): Snapshot<A>? {
      return if (rowSet.size() == 0) {
        null
      } else {
        val stateAsJson = JsonObject(rowSet.first().getValue(1).toString())
        val state = json.decodeFromString<State>(stateAsJson.toString()) as A
        Snapshot(state, rowSet.first().getInteger("version"))
      }
    }
    return sqlClient
      .preparedQuery(SQL_SELECT_VERSION)
      .execute(Tuple.of(id))
      .map { pgRow -> snapshot(pgRow) }
  }
}
