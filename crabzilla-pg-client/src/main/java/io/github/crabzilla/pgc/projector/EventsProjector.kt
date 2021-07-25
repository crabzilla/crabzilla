package io.github.crabzilla.pgc.projector

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

/**
 * To project events within the CommandController (same db transaction)
 */
interface EventsProjector {
  fun project(conn: SqlConnection, event: DomainEvent, eventMetadata: EventMetadata): Future<Void>
}
