package io.github.crabzilla.example1.customer

import io.github.crabzilla.example1.customer.CustomersWriteRepository.updateStatus
import io.github.crabzilla.example1.customer.CustomersWriteRepository.upsert
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.PgEventProjector
import io.vertx.core.Future
import io.vertx.sqlclient.SqlConnection

class CustomersPgEventProjector : PgEventProjector {

  override fun project(conn: SqlConnection, record: EventRecord): Future<Void> {
    val (payload, metadata, id) = record.extract()
    return when (payload.getString("type")) {
      "CustomerRegistered" ->
        upsert(conn, id, payload.getString("name"), false)
      "CustomerActivated" ->
        updateStatus(conn, id, true)
      "CustomerDeactivated" ->
        updateStatus(conn, id, false)
      else -> Future.failedFuture("Unknown event $metadata")
    }
  }
}
