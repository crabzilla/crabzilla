package io.github.crabzilla

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple

// TODO decouple from example1 and make it a library scope class
class TestRepository(private val pgPool: PgPool) {

  companion object {
    private const val selectAfterOffset =
      """
      SELECT *
      FROM events
      WHERE sequence > $1
      ORDER BY sequence
      limit $2
    """
  }

  fun scanEvents(afterSequence: Long, numberOfRows: Int): Future<List<JsonObject>> {
    return pgPool.withConnection { client ->
      client.prepare(selectAfterOffset)
        .compose { preparedStatement -> preparedStatement.query().execute(Tuple.of(afterSequence, numberOfRows)) }
        .map { rowSet: RowSet<Row> ->
          rowSet.iterator().asSequence().map { row: Row ->
            val json = JsonObject()
            val payload = row.getJsonObject("event_payload")
            payload.put("type", row.getString("event_type"))
            json.put("sequence", row.getLong("sequence"))
            json.put("event_payload", payload)
            json.put("state_type", row.getString("state_type"))
            json.put("state_id", row.getUUID("state_id").toString())
            json.put("version", row.getInteger("version"))
            json.put("id", row.getString("id").toString())
            json.put("causation_id", row.getString("causation_id")?.toString())
            json.put("correlation_id", row.getString("correlation_id").toString())
            json
          }.toList()
        }
    }
  }

  fun getAllCustomers(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM customer_summary")
      .execute()
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val json = JsonObject()
          json.put("id", row.getUUID("id").toString())
          json.put("name", row.getString("name"))
          json.put("is_active", row.getBoolean("is_active"))
          json
        }.toList()
      }
  }

  fun getAllCommands(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM commands")
      .execute()
      .map { rowSet: RowSet<Row> ->
        rowSet.iterator().asSequence().map { row: Row ->
          val json = JsonObject()
          json.put("state_id", row.getUUID("state_id").toString())
          json.put("causation_id", row.getString("causation_id").toString())
          json.put("last_causation_id", row.getString("last_causation_id").toString())
          json.put("cmd_payload", row.getJsonObject("cmd_payload"))
          json
        }.toList()
      }
  }

  fun getProjections(name: String): Future<Long> {
    return pgPool.query("SELECT sequence FROM subscriptions where name = '$name'")
      .execute()
      .map { rowSet: RowSet<Row> ->
        rowSet.first().getLong(0)
      }
  }
}
