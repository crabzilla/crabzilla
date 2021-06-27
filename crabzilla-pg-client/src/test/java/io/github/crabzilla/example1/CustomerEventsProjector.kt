package io.github.crabzilla.example1

import io.github.crabzilla.pgc.PgcEventsProjector
import io.github.crabzilla.stack.EventMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

object CustomerEventsProjector : PgcEventsProjector<CustomerEvent> {

  override fun project(conn: SqlConnection, event: CustomerEvent, eventMetadata: EventMetadata): Future<Void> {
    val id = eventMetadata.aggregateRootId.id
    return when (event) {
      is CustomerEvent.CustomerRegistered ->
        CustomersWriteRepository.upsert(conn, id, event.name, false)
      is CustomerEvent.CustomerActivated ->
        CustomersWriteRepository.updateStatus(conn, id, true)
      is CustomerEvent.CustomerDeactivated ->
        CustomersWriteRepository.updateStatus(conn, id, false)
    }
  }
}
