package io.github.crabzilla.example1.customer.effects

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.example1.customer.model.CustomerEvent
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
    // since this handle only one event, no matter how many events more within same session, this is called just once
    val viewStateAsJson: Future<JsonObject?> by lazy {
      sqlConnection
        .preparedQuery("SELECT * FROM customer_summary WHERE id = $1")
        .execute(Tuple.of(UUID.fromString(eventMetadata.stateId)))
        .map {
          if (it.size() == 0) null else it.first().toJson()
        }
    }

    return when (event) {
      is CustomerEvent.CustomerRegistered ->
        sqlConnection
          .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3) RETURNING *")
          .execute(Tuple.of(event.id, event.name, false))
          .map {
            if (it.size() == 0) null else it.first().toJson()
          }
      else ->
        viewStateAsJson
    }
  }
}
