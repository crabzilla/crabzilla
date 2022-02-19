package io.github.crabzilla.pgclient.command

import io.github.crabzilla.pgclient.PgClientFactory
import io.github.crabzilla.pgclient.PgClientFactory.DB_CONFIG_TAG
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

private val dbConfig: JsonObject =
  JsonObject()
    .put("port", 5432)
    .put("host", "0.0.0.0")
    .put("database", "ex1_crabzilla")
    .put("user", "user1")
    .put("password", "pwd1")
    .put("pool", JsonObject().put("maxSize", 7)
    )

val config: JsonObject = JsonObject()
  .put(DB_CONFIG_TAG, "dbConfig")
  .put("dbConfig", dbConfig)

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

fun pgPool(vertx: Vertx): PgPool {
  val poolOptions = PgClientFactory.createPoolOptions(config)
  return PgPool.pool(vertx, connectOptions, poolOptions)
}

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
