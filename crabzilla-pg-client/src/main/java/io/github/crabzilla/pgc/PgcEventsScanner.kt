package io.github.crabzilla.pgc

import io.github.crabzilla.stack.AggregateRootId
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.EventsScanner
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

// TODO could receive also a list of aggregate root names to filter interesting events
class PgcEventsScanner(private val sqlClient: SqlClient, private val streamName: String) : EventsScanner {

  private val selectAfterOffset =
    """
      SELECT ar_name, ar_id, event_payload, sequence, id, causation_id, correlation_id
      FROM events
      WHERE sequence > (select last_offset from projections where name = $1)
      ORDER BY sequence
      limit $2
    """

  private val updateOffset =
    "UPDATE projections SET last_offset = $1 WHERE projections.name = $2"

  override fun streamName(): String {
    return streamName
  }

  override fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    return sqlClient
      .preparedQuery(selectAfterOffset)
      .execute(Tuple.of(streamName, numberOfRows))
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val eventMetadata = EventMetadata(
            row.getString("ar_name"),
            AggregateRootId(row.getUUID("ar_id")),
            EventId(row.getUUID("id")),
            CorrelationId(row.getUUID("correlation_id")),
            CausationId(row.getUUID("causation_id")),
            row.getLong("sequence")
          )
          val jsonObject = JsonObject(row.getValue("event_payload").toString())
          EventRecord(eventMetadata, jsonObject)
        }.toList()
      }
  }

  override fun updateOffSet(eventSequence: Long): Future<Void> {
    return sqlClient
      .preparedQuery(updateOffset)
      .execute(Tuple.of(eventSequence, streamName))
      .mapEmpty()
  }
}
