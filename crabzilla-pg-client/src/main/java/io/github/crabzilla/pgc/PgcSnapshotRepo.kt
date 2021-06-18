package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.stack.SnapshotRepository
import io.vertx.core.Future
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
    const val SQL_SELECT_VERSION =
      """ SELECT version, json_content
          FROM SNAPSHOTS 
          WHERE ar_id = $1"""
  }

  override fun get(id: UUID): Future<Snapshot<A>?> {
    fun currentSnapshot(conn: SqlConnection): Future<Snapshot<A>?> {
      fun snapshot(rowSet: RowSet<Row>): Snapshot<A>? {
        return if (rowSet.size() == 0) {
          null
        } else {
          val stateAsJson = JsonObject(rowSet.first().getValue(1).toString())
          val state = AggregateRoot.fromJson<A>(config.json, stateAsJson.encode())
          Snapshot(state, rowSet.first().getInteger("version"))
        }
      }
      return conn
        .preparedQuery(SQL_SELECT_VERSION)
        .execute(Tuple.of(id))
        .map { pgRow -> snapshot(pgRow) }
    }
    return writeModelDb.withConnection { conn: SqlConnection -> currentSnapshot(conn) }
  }
}
