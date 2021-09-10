package io.github.crabzilla.engine.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.stack.command.Snapshot
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection
import java.util.UUID

interface SnapshotRepository<S : State, E : Event> {
  fun get(conn: SqlConnection, id: UUID): Future<Snapshot<S>?>
  fun upsert(conn: SqlConnection, id: UUID, originalVersion: Int, resultingVersion: Int, newState: S): Future<Void>
}
