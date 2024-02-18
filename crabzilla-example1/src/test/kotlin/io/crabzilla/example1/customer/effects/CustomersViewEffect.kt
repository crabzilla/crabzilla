package io.crabzilla.example1.customer.effects

import io.crabzilla.context.EventRecord
import io.crabzilla.subscription.SubscriptionApiViewEffect
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

class CustomersViewEffect : SubscriptionApiViewEffect {
  override fun handle(
    sqlConnection: SqlConnection,
    eventRecord: EventRecord,
  ): Future<JsonObject?> {
    val (payload, metadata) = eventRecord.extract()
    val idAsUUID = UUID.fromString(metadata.stateId)

    fun updateStatus(isActive: Boolean): Future<JsonObject?> {
      return sqlConnection
        .preparedQuery("UPDATE customer_summary set is_active = $2 WHERE id = $1 RETURNING * ")
        .execute(Tuple.of(idAsUUID, isActive))
        .map { it.first().toJson() }
    }
    return when (payload.getString("type")) {
      "CustomerActivated" ->
        updateStatus(true)
      "CustomerDeactivated" ->
        updateStatus(false)
      "CustomerRenamed" -> TODO()
      else -> Future.succeededFuture()
    }
  }
}
