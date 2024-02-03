package io.github.crabzilla.subscription.internal

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.subscription.SubscriptionCantBeLockedException
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple

internal class SubscriptionDao(
  val sqlConnection: SqlConnection,
  private val name: String,
  private val querySpecification: String,
) {
  fun getOffsets(): Future<SubscriptionOffsets> {
    return sqlConnection
      .preparedQuery(SQL_SELECT_OFFSETS)
      .execute(Tuple.of(name))
      .map {
        val row = it.first()
        SubscriptionOffsets(
          subscriptionOffset = row.getLong("subscription_offset"),
          globalOffset = row.getLong("global_offset") ?: 0,
        )
      }
  }

  fun lockSubscription(): Future<Void> {
    return sqlConnection
      .preparedQuery(SQL_LOCK)
      .execute(Tuple.of("subscriptions_table".hashCode(), name.hashCode()))
      .compose { pgRow ->
        if (pgRow.first().getBoolean("locked")) {
          Future.succeededFuture()
        } else {
          Future.failedFuture(SubscriptionCantBeLockedException("Subscription $name can't be locked"))
        }
      }
  }

  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    return sqlConnection
      .preparedQuery(this.querySpecification)
      .execute(Tuple.of(name, numberOfRows))
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val eventMetadata =
            EventMetadata(
              streamId = row.getInteger("stream_id"),
              stateType = row.getString("state_type"),
              stateId = row.getString("state_id"),
              eventId = row.getUUID("event_id"),
              correlationId = row.getUUID("correlation_id"),
              causationId = row.getUUID("causation_id"),
              eventSequence = row.getLong("sequence"),
              version = row.getInteger("version"),
              eventType = row.getString("event_type"),
            )
          val jsonObject = JsonObject(row.getValue("event_payload").toString())
          EventRecord(eventMetadata, jsonObject)
        }.toList()
      }
  }

  fun updateOffset(
    subscriptionName: String,
    offset: Long,
  ): Future<Void> {
    return sqlConnection
      .preparedQuery(SQL_UPDATE_OFFSET)
      .execute(Tuple.of(subscriptionName, offset))
      .mapEmpty()
  }

  companion object {
    private const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
    private const val SQL_SELECT_OFFSETS = """
      WITH subscription_offset AS (
        SELECT sequence as subscription_offset FROM subscriptions WHERE name = $1
      ), global_offset AS (
        SELECT max(sequence) AS global_offset FROM events
      )
      SELECT subscription_offset.subscription_offset, global_offset.global_offset
        FROM subscription_offset, global_offset
    """
    private const val SQL_UPDATE_OFFSET = "UPDATE subscriptions SET sequence = $2 where name = $1"

    data class SubscriptionOffsets(val subscriptionOffset: Long, val globalOffset: Long)
  }
}
