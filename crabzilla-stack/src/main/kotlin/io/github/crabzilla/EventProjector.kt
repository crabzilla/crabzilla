package io.github.crabzilla

import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

/**
 * To project events
 */
interface EventProjector {
  fun project(conn: SqlConnection, eventRecord: EventRecord): Future<Void>
}
