package io.github.crabzilla.pgc

import io.vertx.core.Future
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

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
    .compose { sqlClient.query("update projections set last_offset = 0").execute() }
    .compose { sqlClient.query("delete from customer_summary").execute() }
    .mapEmpty()
}
