package io.github.crabzilla.command.internal

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import java.util.UUID

sealed interface SnapshotRepository<S : State, E : Event> {
  fun get(conn: SqlClient, id: UUID): Future<Snapshot<S>?>
  fun upsert(conn: SqlClient, id: UUID, originalVersion: Int, resultingVersion: Int, newState: S): Future<Void>
}
