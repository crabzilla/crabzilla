package io.github.crabzilla

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlClient

val testDbConfig: JsonObject =
  JsonObject()
    .put("url", "postgresql://localhost:5432/crabzilla")
    .put("username", "user1")
    .put("password", "pwd1")

fun cleanDatabase(sqlClient: SqlClient): Future<Void> {
  return  sqlClient.query("truncate events, commands, customer_summary restart identity").execute()
    .compose { sqlClient.query("update subscriptions set sequence = 0").execute() }
    .mapEmpty()
}
