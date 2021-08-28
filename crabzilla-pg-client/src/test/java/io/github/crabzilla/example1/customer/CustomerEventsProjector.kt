package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.Event
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.pgc.projector.EventsProjector
import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

object CustomerEventsProjector : EventsProjector {

  override fun project(conn: SqlConnection, event: Event, eventMetadata: EventMetadata): Future<Void> {
    val id = eventMetadata.stateId.id
    val repo = CustomersWriteRepository
    return when (val customerEvent = event as CustomerEvent) {
      is CustomerRegistered ->
        repo.upsert(conn, id, customerEvent.name, false)
      is CustomerActivated ->
        repo.updateStatus(conn, id, true)
      is CustomerDeactivated ->
        repo.updateStatus(conn, id, false)
    }
  }
}
