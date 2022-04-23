package io.github.crabzilla.example1.customer

import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.projection.PgEventProjector
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

class CustomersPgEventProjector : PgEventProjector {

  override fun project(conn: SqlConnection, payload: JsonObject, metadata: EventMetadata): Future<Void> {
    val id = metadata.stateId
    return when (payload.getString("type")) {
      "CustomerRegistered" ->
        CustomersWriteRepository.upsert(conn, id, payload.getString("name"), false)
      "CustomerActivated" ->
        CustomersWriteRepository.updateStatus(conn, id, true)
      "CustomerDeactivated" ->
        CustomersWriteRepository.updateStatus(conn, id, false)
      else -> Future.failedFuture("Unknown event $metadata")
    }
  }
}
