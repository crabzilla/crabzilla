package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.webpgc.ResourceContext
import io.github.crabzilla.webpgc.WebPgcReadContext
import io.github.crabzilla.webpgc.WebPgcWriteContext
import io.github.crabzilla.webpgc.addProjector
import io.github.crabzilla.webpgc.addResourceForEntity
import io.github.crabzilla.webpgc.listenHandler
import io.github.crabzilla.webpgc.readModelPgPool
import io.github.crabzilla.webpgc.writeModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json

class CustomerVerticle : AbstractVerticle() {

  val httpPort: Int by lazy { config().getInteger("HTTP_PORT") }
  val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }
  val writeDb: PgPool by lazy { writeModelPgPool(vertx, config()) }
  lateinit var server: HttpServer

  override fun start(promise: Promise<Void>) {

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // kotlinx serialization
    val example1Json = Json(context = customerModule)

    // web command routes
    val cmdTypeMap = mapOf(
      Pair("create", CreateCustomer::class.qualifiedName as String),
      Pair("activate", ActivateCustomer::class.qualifiedName as String),
      Pair("deactivate", DeactivateCustomer::class.qualifiedName as String),
      Pair("create-activate", CreateActivateCustomer::class.qualifiedName as String))

    val webPgcWriteContext = WebPgcWriteContext(vertx, example1Json, writeDb)
    val resourceContext = ResourceContext("customers", "customer", CustomerCommandAware(), cmdTypeMap)
    addResourceForEntity(webPgcWriteContext, resourceContext, router)

    // projection consumers
    val webPgcReadContext = WebPgcReadContext(vertx, example1Json, readDb)
    addProjector(webPgcReadContext, "customers-summary", CustomerProjector())

    // read model routes
    router.get("/customers/:id").handler(::customersQueryHandler)

    // vertx http
    server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen(listenHandler(promise))
  }

  private fun customersQueryHandler(rc: RoutingContext) {
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
      rc.response()
        .putHeader("Content-type", "application/json")
        .end(array.getJsonObject(0).encode())
    }
  }
}
