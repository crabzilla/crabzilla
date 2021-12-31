package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.projection.EventsProjector
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

class CustomerEventsProjector(override val viewName: String) : EventsProjector {

  private val repo = CustomersWriteRepository(viewName)

  override fun project(conn: SqlConnection, eventAsJson: JsonObject, eventMetadata: EventMetadata): Future<Void> {
    val id = eventMetadata.stateId.id
    return when (eventAsJson.getString("type")) {
      "CustomerRegistered" ->
        repo.upsert(conn, id, eventAsJson.getString("name"), false)
      "CustomerActivated" ->
        repo.updateStatus(conn, id, true)
      "CustomerDeactivated" ->
        repo.updateStatus(conn, id, false)
      else -> failedFuture("Unknown event")
    }
  }
}
