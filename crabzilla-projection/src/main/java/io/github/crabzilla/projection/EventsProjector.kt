package io.github.crabzilla.projection

import io.github.crabzilla.core.metadata.EventMetadata
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
