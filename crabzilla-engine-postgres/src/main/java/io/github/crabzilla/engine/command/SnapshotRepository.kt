package io.github.crabzilla.engine.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.stack.command.Snapshot
import io.vertx.core.Future
import io.vertx.pgclient.PgConnection
import java.util.UUID

interface SnapshotRepository<S : State, E : Event> {
  fun get(pgConn: PgConnection, id: UUID): Future<Snapshot<S>?>
  fun upsert(conn: PgConnection, id: UUID, originalVersion: Int, resultingVersion: Int, newState: S): Future<Void>
}
