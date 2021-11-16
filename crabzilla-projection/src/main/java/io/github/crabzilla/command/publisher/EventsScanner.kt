package io.github.crabzilla.command.publisher

import io.github.crabzilla.command.EventRecord
import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.EventId
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

// TODO could receive also a list of aggregate root names to filter interesting events
internal class EventsScanner(
  private val sqlClient: SqlClient,
  private val name: String
) {

  companion object {
    private val log = LoggerFactory.getLogger(EventsScanner::class.java)
    private const val selectAfterOffset =
      """
      SELECT ar_name, ar_id, event_payload, sequence, id, causation_id, correlation_id
      FROM events
      WHERE sequence > (select sequence from publications where name = $1)
      ORDER BY sequence
      limit $2
    """
    private const val updateOffset =
      "UPDATE publications SET sequence = $1 WHERE publications.name = $2"
  }

  init {
    log.info("Starting for [{}}]", name)
  }

  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    log.debug("Scanning for new events on stream {}", name)
    return sqlClient
      .preparedQuery(selectAfterOffset)
      .execute(Tuple.of(name, numberOfRows))
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val eventMetadata = EventMetadata(
            row.getString("ar_name"),
            StateId(row.getUUID("ar_id")),
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
