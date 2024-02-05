package io.github.crabzilla.customer

import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.ViewEffect
import io.github.crabzilla.example1.customer.CustomerEvent
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

/**
 * This is in order to project other events to customer view. Notice that CustomerRegistered is not handled here
 */
class CustomersAsyncViewEffect : ViewEffect {
  private val serDer = CustomerEventSerDer()

  override fun handleEffect(
    sqlConnection: SqlConnection,
    eventRecord: EventRecord,
  ): Future<JsonObject?> {
    val (payload, metadata) = eventRecord.extract()
    val idAsUUID = UUID.fromString(metadata.stateId)
    return when (val event = serDer.fromJson(payload)) {
      is CustomerEvent.CustomerActivated ->
        updateStatus(sqlConnection, idAsUUID, true)
      is CustomerEvent.CustomerDeactivated ->
        updateStatus(sqlConnection, idAsUUID, false)
      is CustomerEvent.CustomerRenamed -> TODO()
      else -> Future.succeededFuture(null)
    }
  }

  companion object {
    fun updateStatus(
      conn: SqlConnection,
      id: UUID,
      isActive: Boolean,
    ): Future<JsonObject?> {
      return conn
        .preparedQuery("UPDATE customer_summary set is_active = $2 WHERE id = $1 RETURNING * ")
        .execute(Tuple.of(id, isActive))
        .map { it.first().toJson() }
    }
  }
}
