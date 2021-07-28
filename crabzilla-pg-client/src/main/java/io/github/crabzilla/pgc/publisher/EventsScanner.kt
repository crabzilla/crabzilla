package io.github.crabzilla.pgc.publisher

import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.DomainStateId
import io.github.crabzilla.stack.EventId
import io.github.crabzilla.stack.EventMetadata
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

// TODO could receive also a list of aggregate root names to filter interesting events
class EventsScanner(
  private val sqlClient: SqlClient,
  private val name: String,
  publicationType: String
) {

  companion object {
    private val log = LoggerFactory.getLogger(EventsScanner::class.java)
  }

  private val table = when (publicationType) {
    "projection" -> "projections"
    "publication" -> "publications"
    else -> throw IllegalArgumentException("Invalid publicationType $publicationType")
  }

  private val selectAfterOffset =
    """
      SELECT ar_name, ar_id, event_payload, sequence, id, causation_id, correlation_id
      FROM events
      WHERE sequence > (select sequence from $table where name = $1)
      ORDER BY sequence
      limit $2
    """

  private val updateOffset =
    "UPDATE $table SET sequence = $1 WHERE $table.name = $2"

  fun streamName(): String {
    return name
  }

  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    log.debug("Scanning for new events on stream {}", streamName())
    return sqlClient
      .preparedQuery(selectAfterOffset)
      .execute(Tuple.of(name, numberOfRows))
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

  fun updateOffSet(eventSequence: Long): Future<Void> {
    return sqlClient
      .preparedQuery(updateOffset)
      .execute(Tuple.of(eventSequence, name))
      .mapEmpty()
  }
}
