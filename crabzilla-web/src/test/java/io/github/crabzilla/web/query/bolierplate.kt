package io.github.crabzilla.web.query

import io.github.crabzilla.web.example1.ActivateCustomer
import io.github.crabzilla.web.example1.CreateActivateCustomer
import io.github.crabzilla.web.example1.CreateCustomer
import io.github.crabzilla.web.example1.DeactivateCustomer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple

// customer web command routes
val cmdTypeMapOfCustomer = mapOf(
  Pair("create", CreateCustomer::class.qualifiedName as String),
  Pair("activate", ActivateCustomer::class.qualifiedName as String),
  Pair("deactivate", DeactivateCustomer::class.qualifiedName as String),
  Pair("create-activate", CreateActivateCustomer::class.qualifiedName as String))

// query routes
fun customersQueryHandler(rc: RoutingContext, readDb: PgPool) {
  val sql = """SELECT id, name, is_active FROM customer_summary where id = $1""".trimMargin()
  val id = rc.request().getParam("id").toInt()
  readDb.preparedQuery(sql).execute(Tuple.of(id)) { event ->
    if (event.failed()) {
      rc.response()
        .putHeader("Content-type", "application/json")
        .setStatusCode(500)
        .setStatusMessage(event.cause().message)
        .end()
    }
    val set = event.result()
    val array = JsonArray()
    for (row in set) {
      val jo =
        JsonObject().put("id", row.getInteger(0)).put("name", row.getString(1)).put("is_active", row.getBoolean(2))
      array.add(jo)
    }
    if (array.isEmpty) {
      println("** NOT found id $id")
      rc.response()
        .putHeader("Content-type", "application/json")
        .setStatusCode(404)
        .end()
    } else {
      println("** found id $id: ${array.getJsonObject(0)}")
      rc.response()
        .putHeader("Content-type", "application/json")
        .end(array.getJsonObject(0).encode())
    }
  }
}
