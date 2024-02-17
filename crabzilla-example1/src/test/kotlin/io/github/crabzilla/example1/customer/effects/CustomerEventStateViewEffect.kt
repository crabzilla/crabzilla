package io.github.crabzilla.example1.customer.effects

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.example1.customer.model.CustomerEvent
import io.github.crabzilla.writer.ViewEffect.EventStateViewEffect
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

class CustomerEventStateViewEffect :
  EventStateViewEffect<Customer, CustomerEvent> {
  override fun handle(
    sqlConnection: SqlConnection,
    event: CustomerEvent,
    state: Customer,
    eventMetadata: EventMetadata,
  ): Future<JsonObject?> {
    var viewStateAsJson: JsonObject? = null

    return when (event) {
      is CustomerEvent.CustomerRegistered ->
        sqlConnection
          .preparedQuery("INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3) RETURNING *")
          .execute(Tuple.of(event.id, event.name, false))
          .map { if (it.size() == 0) null else it.first().toJson() }
          .onSuccess {
            viewStateAsJson = it!!
          }
      else ->
        Future.succeededFuture(viewStateAsJson)
    }
  }
}
