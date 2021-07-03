package io.github.crabzilla.example1

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.integration.EventsProjector
import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

object CustomerEventsProjector : EventsProjector {

  override fun project(conn: SqlConnection, event: DomainEvent, eventMetadata: EventMetadata): Future<Void> {
    val id = eventMetadata.aggregateRootId.id
    return when (val customerEvent = event as CustomerEvent) {
      is CustomerEvent.CustomerRegistered ->
        CustomersWriteRepository.upsert(conn, id, customerEvent.name, false)
      is CustomerEvent.CustomerActivated ->
        CustomersWriteRepository.updateStatus(conn, id, true)
      is CustomerEvent.CustomerDeactivated ->
        CustomersWriteRepository.updateStatus(conn, id, false)
    }
  }
}
