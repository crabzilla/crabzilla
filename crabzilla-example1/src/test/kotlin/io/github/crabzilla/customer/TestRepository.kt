package io.github.crabzilla.customer

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

class TestRepository(private val pgPool: Pool) {
  fun cleanDatabase(sqlClient: SqlClient): Future<Void> {
    return sqlClient.query("truncate streams, events, commands, customer_summary restart identity").execute()
      .compose { sqlClient.query("update subscriptions set sequence = 0").execute() }
      .mapEmpty()
  }

  fun getAllEvents(
    afterSequence: Long = 0,
    numberOfRows: Int = Int.MAX_VALUE,
  ): Future<List<JsonObject>> {
    return pgPool.withConnection { client ->
      client.prepare(SELECT_AFTER_OFFSET)
        .compose { preparedStatement -> preparedStatement.query().execute(Tuple.of(afterSequence, numberOfRows)) }
        .map { rowSet ->
          rowSet.map {
            it.toJson()
          }
        }
    }
  }

  fun getAllCustomers(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM customer_summary")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }
  }

  fun getAllCommands(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM commands")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }
  }

  fun getSubscriptions(name: String): Future<List<JsonObject>> {
    return pgPool.query("SELECT sequence FROM subscriptions where name = '$name'")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
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

    val DATABASE_CONFIG: JsonObject =
      JsonObject()
        .put("url", "postgresql://localhost:5432/crabzilla")
        .put("username", "user1")
        .put("password", "pwd1")
  }
}
