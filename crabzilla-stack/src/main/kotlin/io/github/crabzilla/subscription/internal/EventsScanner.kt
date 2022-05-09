package io.github.crabzilla.subscription.internal

import io.github.crabzilla.EventMetadata
import io.github.crabzilla.EventRecord
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

internal class EventsScanner(
  private val pgPool: PgPool,
  private val name: String,
  private val querySpecification: String
) {

  companion object {
    private val log = LoggerFactory.getLogger(EventsScanner::class.java)
    private const val selectCurrentOffset = """
      select sequence from subscriptions where name = $1
    """
    private const val selectGlobalOffset = """
      select max(sequence) as sequence from events
    """
  }

  init {
    log.info("Starting for subscription {}", name)
  }

  fun getCurrentOffset(): Future<Long> {
    return pgPool
      .preparedQuery(selectCurrentOffset)
      .execute(Tuple.of(name))
      .map {
        it.first().getLong("sequence")
      }
  }

  fun getGlobalOffset(): Future<Long> {
    return pgPool
      .preparedQuery(selectGlobalOffset)
      .execute()
      .map {
        if (it.rowCount() == 1) it.first().getLong("sequence") ?: 0L else 0L
      }
  }

  fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    return pgPool
      .preparedQuery(querySpecification)
      .execute(Tuple.of(name, numberOfRows))
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val eventMetadata = EventMetadata(
            row.getString("state_type"),
            row.getUUID("state_id"),
            row.getUUID("id"),
            row.getUUID("correlation_id"),
            row.getUUID("causation_id"),
            row.getLong("sequence"),
            row.getInteger("version"),
            row.getString("event_type")
          )
          val jsonObject = JsonObject(row.getValue("event_payload").toString())
          EventRecord(eventMetadata, jsonObject)
        }.toList()
      }
  }
}
