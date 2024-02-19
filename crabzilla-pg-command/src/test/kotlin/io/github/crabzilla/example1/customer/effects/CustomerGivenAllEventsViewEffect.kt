package io.github.crabzilla.example1.customer.effects

import io.github.crabzilla.command.CommandHandlerResult
import io.github.crabzilla.command.ViewEffect
import io.github.crabzilla.context.CrabzillaRuntimeException
import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.example1.customer.model.CustomerEvent
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.*

class CustomerGivenAllEventsViewEffect :
  ViewEffect.GivenAllEventsViewEffect<Customer, CustomerEvent> {
  override fun handle(
    sqlConnection: SqlConnection,
    result: CommandHandlerResult<Customer, CustomerEvent>,
  ): Future<JsonObject?> {
    fun upsert(tuple: Tuple): Future<JsonObject?> {
      return sqlConnection
        .preparedQuery(
          "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3) " +
            " ON CONFLICT (id) " +
            " DO UPDATE SET name = $2, is_active = $3 " +
            " RETURNING *",
        )
        .execute(tuple)
        .map { if (it.size() == 0) null else it.first().toJson() }
    }
    val id = UUID.fromString(result.metadata.last().stateId)
    return when (val state = result.snapshot.state) {
      is Customer.Active -> upsert(Tuple.of(id, state.name, true))
      is Customer.Inactive -> upsert(Tuple.of(id, state.name, false))
      is Customer.Initial -> throw CrabzillaRuntimeException("Invalid state: $state")
    }
  }
}
