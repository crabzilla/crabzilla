package io.github.crabzilla.pgc.integration

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

/**
 * To project events within the PgcEventStore (same db transaction)
 */
interface EventsProjector {
  fun project(conn: SqlConnection, event: DomainEvent, eventMetadata: EventMetadata): Future<Void>
}
