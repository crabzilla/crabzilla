package io.github.crabzilla.accounts.projectors.accounts

import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.pgclient.EventsProjector
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.util.UUID

class AccountsProjector(override val viewName: String) : EventsProjector {

  override fun project(conn: SqlConnection, eventAsJson: JsonObject, eventMetadata: EventMetadata): Future<Void> {
    fun updateBalance(conn: SqlConnection, id: UUID, finalBalance: Double) : Future<Void> {
      return conn
        .preparedQuery("update $viewName set balance = $2 where id = $1")
        .execute(Tuple.of(id, finalBalance))
        .map { it.size() == 1 }
        .mapEmpty()
    }

    val id = eventMetadata.stateId
    return when (val eventName = eventAsJson.getString("type")) {
      "MoneyDeposited" ->
        updateBalance(conn, id, eventAsJson.getDouble("finalBalance"))
      "MoneyWithdrawn" ->
        updateBalance(conn, id, eventAsJson.getDouble("finalBalance"))
      else ->
        failedFuture("Unknown event $eventName")
    }
  }

}