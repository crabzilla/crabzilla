package io.github.crabzilla.pgc.publisher

import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.DomainStateId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.publisher.EventsScanner
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

// TODO could receive also a list of aggregate root names to filter interesting events
class PgcEventsScanner(private val sqlClient: SqlClient, private val projectionName: String) : EventsScanner {

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventsScanner::class.java)
  }

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
    return projectionName
  }

  override fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    log.debug("Scanning for new events on stream {}", streamName())
    return sqlClient
      .preparedQuery(selectAfterOffset)
      .execute(Tuple.of(projectionName, numberOfRows))
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val eventMetadata = EventMetadata(
            row.getString("ar_name"),
            DomainStateId(row.getUUID("ar_id")),
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
      .execute(Tuple.of(eventSequence, projectionName))
      .mapEmpty()
  }
}
