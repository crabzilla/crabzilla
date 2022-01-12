package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.pgclient.EventsProjector
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

class CustomersEventsProjector(override val viewName: String) : EventsProjector {

  override fun project(conn: SqlConnection, eventAsJson: JsonObject, eventMetadata: EventMetadata): Future<Void> {
    val id = eventMetadata.stateId
    return when (eventAsJson.getString("type")) {
      "CustomerRegistered" ->
        CustomersWriteRepository.upsert(conn, id, eventAsJson.getString("name"), false)
      "CustomerActivated" ->
        CustomersWriteRepository.updateStatus(conn, id, true)
      "CustomerDeactivated" ->
        CustomersWriteRepository.updateStatus(conn, id, false)
      else -> Future.failedFuture("Unknown event $eventMetadata")
    }
  }
}
