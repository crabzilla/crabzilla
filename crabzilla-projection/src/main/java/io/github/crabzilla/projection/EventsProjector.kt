package io.github.crabzilla.projection

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.metadata.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

/**
 * To project events
 */
interface EventsProjector {
  fun project(conn: SqlConnection, event: Event, eventMetadata: EventMetadata): Future<Void>
}
