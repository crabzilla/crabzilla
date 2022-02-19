package io.github.crabzilla.pgclient.command.internal

import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import java.util.UUID

sealed interface SnapshotRepository<S : Any, E : Any> {
  fun get(conn: SqlConnection, id: UUID): Future<Snapshot<S>?>
  fun upsert(conn: SqlConnection, id: UUID, originalVersion: Int, resultingVersion: Int, newState: S): Future<Void>
}
