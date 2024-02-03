package io.github.crabzilla.util

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
      .map { JsonObject().put("streams", it) }
      .compose { json ->
        getEvents(0, 1000).map { json.put("events", it) }
      }
      .compose { json ->
        getCommands().map { json.put("commands", it) }
      }
      .compose { json ->
        getSubscriptions().map { json.put("subscriptions", it) }
      }
      .compose { json ->
        getCustomers().map { json.put("customers-view", it) }
      }
      .onComplete {
        println("-------------------------- Crabzilla state overview")
        println(it.result().encodePrettily())
      }
  }

  fun getStreams(): Future<List<JsonObject>> {
    return pgPool.query("select * from streams")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }.map {
        it ?: emptyList()
      }
  }

  fun getEvents(
    afterSequence: Long,
    numberOfRows: Int,
  ): Future<List<JsonObject>> {
    return pgPool.withConnection { client ->
      client.prepare(SQL_SELECT_AFTER_OFFSET)
        .compose { preparedStatement -> preparedStatement.query().execute(Tuple.of(afterSequence, numberOfRows)) }
        .map { rowSet ->
          rowSet.map {
            it.toJson()
          }
        }.map {
          it ?: emptyList()
        }
    }
  }

  fun getCustomers(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM customer_summary")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }.map {
        it ?: emptyList()
      }
  }

  fun getCommands(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM commands")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }.map {
        it ?: emptyList()
      }
  }

  fun getSubscriptions(): Future<List<JsonObject>> {
    return pgPool.query("SELECT * FROM subscriptions ORDER BY name")
      .execute()
      .map { rowSet ->
        rowSet.map {
          it.toJson()
        }
      }.map {
        it ?: emptyList()
      }
  }

  fun getSubscription(name: String): Future<Long> {
    return pgPool.query("SELECT sequence FROM subscriptions where name = '$name'")
      .execute()
      .map { rowSet: RowSet<Row> ->
        rowSet.first().getLong(0)
      }
  }

  companion object {
    private const val SQL_SELECT_AFTER_OFFSET =
      """
      SELECT *
      FROM events
      WHERE sequence > $1
      ORDER BY sequence
      limit $2
    """
  }
}
