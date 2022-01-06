package io.github.crabzilla.pgclient.command.internal

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import java.util.UUID

sealed interface SnapshotRepository<S : State, E : Event> {
  fun get(conn: SqlConnection, id: UUID): Future<Snapshot<S>?>
  fun upsert(conn: SqlConnection, id: UUID, originalVersion: Int, resultingVersion: Int, newState: S): Future<Void>
}
