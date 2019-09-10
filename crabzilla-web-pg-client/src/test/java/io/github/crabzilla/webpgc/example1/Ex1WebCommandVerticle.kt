package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.webpgc.WebCommandVerticle
import io.github.crabzilla.webpgc.listenHandler
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

class Ex1WebCommandVerticle : WebCommandVerticle() {

  lateinit var server: HttpServer

  companion object {
    internal var log = getLogger(Ex1WebCommandVerticle::class.java)
    init {
      System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
        SLF4JLogDelegateFactory::class.java.name)
      org.slf4j.LoggerFactory.getLogger(LoggerFactory::class.java)
    }
  }

  override fun start(startFuture: Future<Void>) {

    val router = Router.router(vertx)

    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    addResourceForEntity("customers", "customer", CustomerJsonAware(), CustomerCommandAware(), router)

    server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))

    server.requestHandler(router).listen(listenHandler(startFuture))

  }

}

