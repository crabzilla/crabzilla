package io.github.crabzilla.example1.customer

import io.github.crabzilla.EventProjector
import io.github.crabzilla.EventRecord
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.CustomersWriteRepository.updateStatus
import io.github.crabzilla.example1.customer.CustomersWriteRepository.upsert
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

class CustomersEventProjector : EventProjector {

  private val serDer = CustomerJsonObjectSerDer()

  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    val (payload, _, id) = record.extract()
    return when (val event = serDer.eventFromJson(payload)) {
      is CustomerRegistered ->
        upsert(conn, id, event.name, false)
      is CustomerActivated ->
        updateStatus(conn, id, true)
      is CustomerDeactivated ->
        updateStatus(conn, id, false)
    }
  }
}
