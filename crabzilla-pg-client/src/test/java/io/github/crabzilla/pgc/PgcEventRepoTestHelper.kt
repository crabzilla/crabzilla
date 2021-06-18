package io.github.crabzilla.pgc

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import java.util.UUID

class PgcEventRepoTestHelper(private val writeModelDb: PgPool) {

  companion object {
    private const val SQL_UPSERT_VERSION =
      """ INSERT INTO snapshots (ar_id, ar_type, version, json_content)
          VALUES ($1, $2, $3, $4)
          ON CONFLICT (ar_id, ar_type) DO UPDATE SET version = $3, json_content = $4"""
    private const val selectAfterOffset =
      """
      SELECT *
      FROM events
      WHERE sequence > $1
      ORDER BY sequence
      limit $2
    """
  }

  fun upsert(id: UUID, type: String, version: Int, snapshotAsJson: JsonObject): Future<Void> {
    val tuple = Tuple.of(id, type, version, snapshotAsJson)
    return writeModelDb.preparedQuery(SQL_UPSERT_VERSION).execute(tuple).mapEmpty()
  }

  fun scanEvents(afterSequence: Long, numberOfRows: Int): Future<List<JsonObject>> {
    return writeModelDb.withConnection { client ->
      client.prepare(selectAfterOffset)
        .compose { preparedStatement -> preparedStatement.query().execute(Tuple.of(afterSequence, numberOfRows)) }
        .map { rowSet: RowSet<Row> ->
          rowSet.iterator().asSequence().map { row: Row ->
            /**
             * {
             "sequence" : 19,
             "event_payload" : {
             "id" : "21581486-8d89-4304-b0ad-6f681c650a2e",
             "name" : "c1",
             "type" : "CustomerRegistered"
             },
             "ar_name" : "Customer",
             "ar_id" : "21581486-8d89-4304-b0ad-6f681c650a2e",
             "version" : 1,
             "id" : "7ddec748-95da-4005-8acb-a4d53b6f9226",
             "causation_id" : "d045279c-9d80-424f-b39e-8769d76ca369",
             "correlation_id" : "d045279c-9d80-424f-b39e-8769d76ca369",
             "inserted_on" : "2021-06-18T18:42:16.535003"
             }
             */
            val json = JsonObject()
            json.put("sequence", row.getLong("sequence"))
            json.put("event_payload", row.getValue("event_payload").toString())
            json.put("ar_name", row.getString("ar_name"))
            json.put("ar_id", row.getUUID("ar_id").toString())
            json.put("version", row.getInteger("version"))
            json.put("id", row.getUUID("id").toString())
            json.put("causation_id", row.getUUID("causation_id").toString())
            json.put("correlation_id", row.getUUID("correlation_id").toString())
            json
          }.toList()
        }
    }
  }
}
