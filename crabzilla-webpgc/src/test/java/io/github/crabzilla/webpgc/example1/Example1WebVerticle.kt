package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.webpgc.WebCommandVerticle
import io.vertx.core.Future
import io.vertx.core.Launcher
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

// Convenience method so you can run it in your IDE
fun main() {
  Launcher.executeCommand("run", Example1WebVerticle::class.java.name)
}

class Example1WebVerticle : WebCommandVerticle() {

  lateinit var server: HttpServer

  companion object {
    internal var log = getLogger(Example1WebVerticle::class.java)
    init {
      System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
        SLF4JLogDelegateFactory::class.java.name)
      org.slf4j.LoggerFactory.getLogger(LoggerFactory::class.java)
    }
  }

  override fun start(startFuture: Future<Void>) {

    val httpPort = config().getInteger("HTTP_PORT")

    val router = Router.router(vertx)

    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    this.addResourceForEntity("customers", "customer", CustomerJsonAware(), CustomerCommandAware(), router)

    // read model routes
    router.get("/customers/:id").handler {
        it.response()
          .putHeader("Content-type", "application/json")
          .end(JsonObject().put("message", "TODO query read model").encode())
    }

    server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))

    server.requestHandler(router).listen { startedFuture ->
      if (startedFuture.succeeded()) {
        log.info("Server started on port " + startedFuture.result().actualPort())
        startFuture.complete()
      } else {
        log.error("oops, something went wrong during server initialization", startedFuture.cause())
        startFuture.fail(startedFuture.cause())
      }
    }

  }

}

