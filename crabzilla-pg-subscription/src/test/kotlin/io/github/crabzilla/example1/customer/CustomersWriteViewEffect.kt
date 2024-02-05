package io.github.crabzilla.example1.customer

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.writer.WriteApiEventViewEffect
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

class CustomersWriteViewEffect : WriteApiEventViewEffect<CustomerEvent> {
  override fun handle(
    sqlConnection: SqlConnection,
    eventMetadata: EventMetadata,
    event: CustomerEvent,
  ): Future<JsonObject?> {
    val viewStateAsJson: Future<JsonObject?> by lazy { // no matter how many events, this is called just once
      sqlConnection
        .preparedQuery("SELECT * FROM customer_summary WHERE id = $1")
        .execute(Tuple.of(UUID.fromString(eventMetadata.stateId)))
        .map {
          it.first().toJson()
        }
    }

    return when (event) {
      is CustomerEvent.CustomerRegistered ->
        sqlConnection
          .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3) RETURNING *")
          .execute(Tuple.of(event.id, event.name, false))
          .map {
            it.first().toJson()
          }
      else ->
        viewStateAsJson
    }
  }
}
