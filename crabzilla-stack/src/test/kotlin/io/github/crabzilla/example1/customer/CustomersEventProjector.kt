package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.customer.CustomerEvent.*
import io.github.crabzilla.example1.customer.CustomersWriteDao.updateName
import io.github.crabzilla.example1.customer.CustomersWriteDao.updateStatus
import io.github.crabzilla.example1.customer.CustomersWriteDao.upsert
import io.github.crabzilla.stack.EventProjector
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

class CustomersEventProjector : EventProjector {

  private val serDer = CustomerJsonObjectSerDer()

  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    val (payload, _, id) = record.extract()
    return when (val event = serDer.eventFromJson(payload)) {
      is CustomerRegistered ->
        upsert(conn, id,false)
      is CustomerRegisteredPrivate ->
        updateName(conn, id, event.name)
      is CustomerActivated ->
        updateStatus(conn, id, true)
      is CustomerDeactivated ->
        updateStatus(conn, id, false)
    }
  }
}
