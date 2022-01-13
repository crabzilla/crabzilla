package io.github.crabzilla.accounts.projectors.transfers

import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.pgclient.EventsProjector
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID

class TransferProjector : EventsProjector {

  companion object {
    private val log = LoggerFactory.getLogger(TransferProjector::class.java)
  }

  override val viewName: String
    get() = "transfers_view"

  override fun project(conn: SqlConnection, eventAsJson: JsonObject, eventMetadata: EventMetadata): Future<Void> {
    fun request(id: UUID): Future<Void> {
      val tuple = Tuple.of(id,
        eventAsJson.getDouble("amount"),
        UUID.fromString(eventAsJson.getString("fromAccountId")),
        UUID.fromString(eventAsJson.getString("toAccountId")),
        eventMetadata.eventId,
        eventMetadata.correlationId
      )
      log.info("Will project new transfer {}", tuple.deepToString())
      return conn
        .preparedQuery("insert into " +
                "transfers_view (id, amount, from_acct_id, to_acct_id, causation_id, correlation_id) " +
                "values ($1, $2, $3, $4, $5, $6)")
        .execute(tuple)
        .mapEmpty()
    }
    fun register(id: UUID): Future<Void> {
      val tuple = Tuple.of(id,
        eventAsJson.getBoolean("succeeded"),
        eventAsJson.getString("errorMessage"),
        eventMetadata.eventId,
        eventMetadata.correlationId
      )
      log.info("Will project transfer result {}", tuple.deepToString())
      return conn
        .preparedQuery("update transfers_view " +
                "set pending = false, succeeded = $2, error_message = $3, causation_id = $4, correlation_id = $5 " +
                "where id = $1")
        .execute(tuple)
        .mapEmpty()
    }

    val id = eventMetadata.stateId
    return when (val eventName = eventAsJson.getString("type")) {
      "TransferRequested" -> request(id)
      "TransferConcluded" -> register(id)
      else -> Future.failedFuture("Unknown event $eventName")
    }
  }

}