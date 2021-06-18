package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.EventsScanner
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple

// TODO could receive also a list of aggregate root names to filter interesting events
class PgcEventsScanner(private val writeModelDb: PgPool, private val streamName: String) : EventsScanner {

  private val selectAfterOffset =
    """
      SELECT ar_name, ar_id, event_payload, sequence
      FROM events
      WHERE sequence > (select last_offset from projections where name = $1)
      ORDER BY sequence
      limit $2
    """.trimIndent()

  private val updateOffset =
    "UPDATE projections SET last_offset = $1 WHERE projections.name = $2"

  override fun streamName(): String {
    return streamName
  }

  override fun scanPendingEvents(numberOfRows: Int): Future<List<EventRecord>> {
    return writeModelDb.withConnection { client ->
      client.prepare(selectAfterOffset)
        .compose { preparedStatement -> preparedStatement.query().execute(Tuple.of(streamName, numberOfRows)) }
        .map { rowSet: RowSet<Row> ->
          rowSet.iterator().asSequence().map { row: Row ->
            val jsonObject = JsonObject(row.getValue(2).toString())
            EventRecord(row.getString(0), row.getUUID(1), jsonObject, row.getLong(3))
          }.toList()
        }
    }
  }

  override fun updateOffSet(eventId: Long): Future<Void> {
    return writeModelDb.withConnection { client ->
      client.prepare(updateOffset)
    }.compose { ps2 ->
      ps2.query().execute(Tuple.of(eventId, streamName))
    }.mapEmpty()
  }
}
