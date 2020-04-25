package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.example1.customer.CustomerCommandAware
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.webpgc.WebCommandVerticle
import io.github.crabzilla.webpgc.listenHandler
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler

class Ex1WebCommandVerticle : WebCommandVerticle() {

  lateinit var server: HttpServer

  override fun start(promise: Promise<Void>) {

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    val cmdTypeMap = mapOf(
      Pair("create", "io.github.crabzilla.example1.customer.CreateCustomer"),
      Pair("activate", "io.github.crabzilla.example1.customer.ActivateCustomer"),
      Pair("deactivate", "io.github.crabzilla.example1.customer.DeactivateCustomer"),
      Pair("create-activate", "io.github.crabzilla.example1.customer.CreateActivateCustomer"))

    addResourceForEntity("customers", "customer", CustomerCommandAware(), cmdTypeMap, example1Json, router)

    server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
    server.requestHandler(router).listen(listenHandler(promise))
  }
}
