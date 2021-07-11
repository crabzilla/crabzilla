package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.projector.EventsProjector
import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

object CustomerEventsProjector : EventsProjector {

  override fun project(conn: SqlConnection, event: DomainEvent, eventMetadata: EventMetadata): Future<Void> {
    val id = eventMetadata.domainStateId.id
    val repo = CustomersWriteRepository
    return when (val customerEvent = event as CustomerEvent) {
      is CustomerEvent.CustomerRegistered ->
        repo.upsert(conn, id, customerEvent.name, false)
      is CustomerEvent.CustomerActivated ->
        repo.updateStatus(conn, id, true)
      is CustomerEvent.CustomerDeactivated ->
        repo.updateStatus(conn, id, false)
    }
  }
}
