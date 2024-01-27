package io.github.crabzilla

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

class TestRepository(private val pgPool: PgPool) {
  fun scanEvents(
    afterSequence: Long,
    numberOfRows: Int,
  ): Future<List<JsonObject>> {
    return pgPool.withConnection { client ->
      client.prepare(SELECT_AFTER_OFFSET)
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
            json.put("id", row.getUUID("id").toString())
            json.put("causation_id", row.getUUID("causation_id")?.toString())
            json.put("correlation_id", row.getUUID("correlation_id").toString())
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
          json.put("state_id", row.getString("state_id"))
          json.put("causation_id", row.getUUID("causation_id")?.toString())
          json.put("correlation_id", row.getUUID("correlation_id")?.toString())
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

  companion object {
    private const val SELECT_AFTER_OFFSET =
      """
      SELECT *
      FROM events
      WHERE sequence > $1
      ORDER BY sequence
      limit $2
    """
  }
}

fun cleanDatabase(sqlClient: SqlClient): Future<Void> {
  return sqlClient.query("truncate events, commands, customer_summary restart identity").execute()
    .compose { sqlClient.query("update subscriptions set sequence = 0").execute() }
    .mapEmpty()
}

val testDbConfig: JsonObject =
  JsonObject()
    .put("url", "postgresql://localhost:5432/crabzilla")
    .put("username", "user1")
    .put("password", "pwd1")
