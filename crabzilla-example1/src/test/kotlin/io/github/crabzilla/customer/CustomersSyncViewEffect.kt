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
 * This is in order to enforce aa uniqueness of ID, name, etc on the same transaction of the command
 */
class CustomersSyncViewEffect : ViewEffect {
  private val serDer = CustomerEventSerDer()

  override fun handleEffect(
    sqlConnection: SqlConnection,
    eventRecord: EventRecord,
  ): Future<JsonObject?> {
    val (payload, metadata) = eventRecord.extract()
    val idAsUUID = UUID.fromString(metadata.stateId)
    return when (val event = serDer.fromJson(payload)) {
      is CustomerEvent.CustomerRegistered ->
        sqlConnection
          .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3) RETURNING *")
          .execute(Tuple.of(idAsUUID, event.name, false))
          .map { it.first().toJson() }
      else -> Future.succeededFuture(null)
    }
  }
}
