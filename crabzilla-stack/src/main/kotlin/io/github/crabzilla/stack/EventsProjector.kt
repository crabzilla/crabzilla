package io.github.crabzilla.stack

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

/**
 * To project events
 */
interface EventsProjector {
  val viewName: String
  fun project(conn: SqlConnection, eventAsJson: JsonObject, eventMetadata: EventMetadata): Future<Void>
}
