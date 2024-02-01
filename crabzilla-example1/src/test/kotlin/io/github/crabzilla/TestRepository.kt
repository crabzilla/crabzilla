package io.github.crabzilla

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple

class TestRepository(private val pgPool: Pool) {
  fun cleanDatabase(): Future<Void> {
    return pgPool.query("truncate streams, events, commands, customer_summary restart identity").execute()
      .compose { pgPool.query("update subscriptions set sequence = 0").execute() }
      .mapEmpty()
  }

  fun printOverview(): Future<JsonObject> {
    return getStreams()
      .flatMap { streams ->
        scanEvents(0, 1000).map { Pair(streams, it) }
          .flatMap { pair -> getCommands().map { Triple(pair.first, pair.second, it) } }
          .map {
            JsonObject()
              .put("commands", it.third)
              .put("streams", it.first)
              .put("events", it.second)
          }.onComplete {
            println("-------------------------- NOW")
            println(it.result().encodePrettily())
          }
      }
  }

  fun getStreams(): Future<List<JsonObject>> {
    return pgPool.query("select * from streams")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }
  }

  fun scanEvents(
    afterSequence: Long,
    numberOfRows: Int,
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

  fun getCommands(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM commands")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }
  }

  fun getSubscriptions(name: String): Future<Long> {
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

    val DATABASE_CONFIG: JsonObject =
      JsonObject()
        .put("url", "postgresql://localhost:5432/crabzilla")
        .put("username", "user1")
        .put("password", "pwd1")
  }
}