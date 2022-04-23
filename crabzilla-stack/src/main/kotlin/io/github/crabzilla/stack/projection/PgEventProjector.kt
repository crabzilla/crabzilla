package io.github.crabzilla.stack.projection

import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

/**
 * To project events
 */
interface PgEventProjector {
  fun project(conn: SqlConnection, payload: JsonObject, metadata: EventMetadata): Future<Void>
}
// TODO receber eventRecord
