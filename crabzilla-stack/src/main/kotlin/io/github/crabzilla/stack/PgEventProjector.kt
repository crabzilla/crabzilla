package io.github.crabzilla.stack

import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

/**
 * To project events
 */
interface PgEventProjector {
  fun project(conn: SqlConnection, eventRecord: EventRecord): Future<Void>
}
