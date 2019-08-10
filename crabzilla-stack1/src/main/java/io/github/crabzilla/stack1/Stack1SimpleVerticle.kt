package io.github.crabzilla.stack1

import io.github.crabzilla.initVertx
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

abstract class Stack1SimpleVerticle : AbstractVerticle() {

  lateinit var server: HttpServer
  lateinit var app: Stack1WebApp

  companion object {
    init {
      System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
        SLF4JLogDelegateFactory::class.java.name)
      getLogger(io.vertx.core.logging.LoggerFactory::class.java)
    }
    internal var log = getLogger(Stack1SimpleVerticle::class.java)
  }

  abstract fun startCrabzilla(config: JsonObject, router: Router) : Stack1WebApp

  override fun start(startFuture: Future<Void>) {

    initVertx(vertx)

    val config = config()
    log.info("*** config: \n" + config.encodePrettily())

    val router = Router.router(vertx)

    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    app = startCrabzilla(config, router)

    val httpPort = config.getInteger("HTTP_PORT")
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

  override fun stop() {
    log.info("*** closing resources")
    server.close()
    app.writeDb.close()
    app.readDb.close()
  }

}

