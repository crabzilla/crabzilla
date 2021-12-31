package io.github.crabzilla.projection.internal

import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.EventId
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.github.crabzilla.projection.EventRecord
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

internal class EventsScanner(
  private val sqlClient: SqlClient,
  private val name: String,
  private val querySpecification: String
) {

  companion object {
    private val log = LoggerFactory.getLogger(EventsScanner::class.java)
    private const val selectCurrentOffset = """
      select sequence from projections where name = $1
    """
    private const val selectGlobalOffset = """
      select max(sequence) as sequence from events
    """
  }

  init {
    log.info("Starting for projection {}", name)
  }

  fun getCurrentOffset(): Future<Long> {
    return sqlClient
      .preparedQuery(selectCurrentOffset)
      .execute(Tuple.of(name))
      .transform {
        if (it.failed() || it.result().size() != 1) {
          failedFuture("Projection $name not found on projections table")
        } else {
          succeededFuture(it.result().first().getLong("sequence"))
        }
      }
  }

  fun getGlobalOffset(): Future<Long> {
    return sqlClient
      .preparedQuery(selectGlobalOffset)
      .execute()
      .map {
        if (it.rowCount() == 1) it.first().getLong("sequence") ?: 0L else 0L
      }
  }

  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    return sqlClient
      .preparedQuery(querySpecification)
      .execute(Tuple.of(name, numberOfRows))
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val eventMetadata = EventMetadata(
            row.getString("state_type"),
            StateId(row.getUUID("state_id")),
            EventId(row.getUUID("id")),
            CorrelationId(row.getUUID("correlation_id")),
            CausationId(row.getUUID("causation_id")),
            row.getLong("sequence")
          )
          val jsonObject = JsonObject(row.getValue("event_payload").toString())
          jsonObject.put("type", row.getString("event_type"))
          EventRecord(eventMetadata, jsonObject)
        }.toList()
      }
  }
}
