package io.github.crabzilla

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlClient

val dbConfig: JsonObject =
  JsonObject()
    .put("url", "postgresql://localhost:5432/ex1_crabzilla")
    .put("host", "0.0.0.0")
    .put("username", "user1")
    .put("password", "pwd1")

fun cleanDatabase(sqlClient: SqlClient): Future<Void> {
  return sqlClient.query("delete from commands").execute()
    .compose { sqlClient.query("delete from events").execute() }
    .compose { sqlClient.query("update projections set sequence = 0").execute() }
    .compose { sqlClient.query("alter sequence events_sequence_seq restart").execute() }
    .compose { sqlClient.query("delete from customer_summary").execute() }
    .mapEmpty()
}
