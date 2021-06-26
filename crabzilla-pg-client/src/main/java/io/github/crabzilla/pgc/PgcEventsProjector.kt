package io.github.crabzilla.pgc

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

/**
 * To project events within the PgcEventStore (same db transaction)
 */
interface PgcEventsProjector<E : DomainEvent> {
  fun project(conn: SqlConnection, event: E, metadata: EventMetadata): Future<Void>
}
