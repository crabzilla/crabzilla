package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.webpgc.WebQueryVerticle
import io.reactiverse.pgclient.Tuple
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

class Ex1WebQueryVerticle : WebQueryVerticle() {

  companion object {
    internal val log = getLogger(Ex1WebQueryVerticle::class.java)
  }

  private lateinit var server: HttpServer

  override fun start(future: Future<Void>) {

    val config = config()
    log.info("*** config: \n" + config.encodePrettily())

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // read model routes
    router.get("/customers/:id").handler(::customersQueryHandler)

    // http server
    server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen { startedFuture ->
      if (startedFuture.succeeded()) {
        log.info("Server started on port " + startedFuture.result().actualPort())
        future.complete()
      } else {
        log.error("oops, something went wrong during server initialization", startedFuture.cause())
        future.fail(startedFuture.cause())
      }
    }

  }

  private fun customersQueryHandler(rc: RoutingContext) {
    val sql = """SELECT id, name, is_active FROM customer_summary where id = $1""".trimMargin()
    println("sql: $sql")
    val id = rc.request().getParam("id").toInt()
    println("id: $id")
    readDb.preparedQuery(sql, Tuple.of(id)) { event ->
      if (event.failed()) {
        rc.response()
          .putHeader("Content-type", "application/json")
          .setStatusCode(500)
          .setStatusMessage(event.cause().message)
          .end()
      }
      val set = event.result()
      println("result: $set")
      val array = JsonArray()
      for (row in set) {
        val jo = JsonObject().put("id", row.getInteger(0))
                             .put("name", row.getString(1))
                             .put("is_active", row.getBoolean(2))
        array.add(jo)
      }
      println("array.encodePrettily(): ${array.encodePrettily()}")
      rc.response()
        .putHeader("Content-type", "application/json")
        .end(array.getJsonObject(0).encode())
    }
  }

}

