package io.github.crabzilla.projection

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

val connectOptionsConfig: JsonObject = JsonObject()
  .put("port", 5432)
  .put("host", "0.0.0.0")
  .put("database", "ex1_crabzilla")
  .put("user", "user1")
  .put("password", "pwd1")

val config = JsonObject()
  .put("ex1_crabzilla-config", connectOptionsConfig)

val connectOptions: PgConnectOptions = PgConnectOptions()
  .setPort(5432)
  .setHost("0.0.0.0")
  .setDatabase("ex1_crabzilla")
  .setUser("user1")
  .setPassword("pwd1")
  .setCachePreparedStatements(true)
  .setReconnectAttempts(2)
  .setReconnectInterval(1000)

val poolOptions: PoolOptions = PoolOptions()
  .setMaxSize(7)

fun cleanDatabase(sqlClient: SqlClient): Future<Void> {
  return sqlClient.query("delete from commands").execute()
    .compose { sqlClient.query("delete from events").execute() }
    .compose { sqlClient.query("delete from snapshots").execute() }
    .compose { sqlClient.query("update publications set sequence = 0").execute() }
    .compose { sqlClient.query("update projections set sequence = 0").execute() }
    .compose { sqlClient.query("alter sequence events_sequence_seq restart").execute() }
    .compose { sqlClient.query("delete from customer_summary").execute() }
    .mapEmpty()
}

fun main() {
  println(connectOptionsConfig.encodePrettily())

  println(connectOptions.toJson().encodePrettily())

  println(poolOptions.toJson().encodePrettily())
}
