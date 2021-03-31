package io.github.crabzilla.pgc

import io.github.crabzilla.core.EventRecord
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

class PgcEventsScanner(private val writeModelDb: PgPool) {
  companion object {
    private val log = LoggerFactory.getLogger(PgcEventsScanner::class.java)
    private const val ROWS_PER_TIME = 100
    private const val SELECT_EVENTS_AFTER_OFFSET =
      "SELECT ar_name, ar_id, event_payload, event_id " +
        "FROM events, events_offset " +
        "WHERE event_id > events_offset.last_offset " +
        "ORDER BY event_id " +
        "LIMIT $1 "
    private const val UPDATE_EVENT_OFFSET = "UPDATE events_offset SET last_offset = $1"
  }

  fun scanPendingEvents(numberOfRows: Int = ROWS_PER_TIME): Future<List<EventRecord>> {
    if (log.isDebugEnabled) log.debug("Will scan for new events")
    val promise = Promise.promise<List<EventRecord>>()
    writeModelDb.withTransaction { client ->
      client.prepare(SELECT_EVENTS_AFTER_OFFSET)
        .compose { preparedStatement -> preparedStatement.query().execute(Tuple.of(numberOfRows)) }
        .map { rowSet: RowSet<Row> ->
          rowSet.iterator().asSequence().map { row: Row ->
            if (log.isDebugEnabled) log.debug("Found ${row.deepToString()}")
            val jsonObject: JsonObject = row.get(JsonObject::class.java, 2)
            EventRecord(row.getString(0), row.getInteger(1), jsonObject, row.getLong(3))
          }.toList()
        }
    }
      .onFailure { err ->
        promise.fail(err)
      }
      .onSuccess {
        promise.complete(it)
      }
    return promise.future()
  }

  fun updateOffSet(eventId: Long): Future<Void> {
    val promise = Promise.promise<Void>()
    writeModelDb.withTransaction { client ->
      client.prepare(UPDATE_EVENT_OFFSET)
    }
      .compose { ps2 ->
        ps2.query().execute(Tuple.of(eventId))
      }
      .onFailure { promise.fail(it) }
      .onSuccess { promise.complete() }
    return promise.future()
  }
}
