package io.github.crabzilla.stack1.example1

import io.github.crabzilla.EventBusUowPublisher
import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.initVertx
import io.github.crabzilla.pgc.PgcEntityComponent
import io.github.crabzilla.pgc.pgPool
import io.github.crabzilla.web.WebEntityComponentImpl
import io.reactiverse.pgclient.PgPool
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Launcher
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger

// Convenience method so you can run it in your IDE
fun main() {
  Launcher.executeCommand("run", Example1WebVerticle::class.java.name)
}

class Example1WebVerticle(val httpPort: Int = 8081, val configFile: String = "./example1.env") : AbstractVerticle() {

  lateinit var server: HttpServer
  lateinit var writeDb: PgPool

  companion object {
    internal var log = getLogger(Example1WebVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {

    log.info("*** starting using HTTP_PORT $httpPort")

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", configFile))

    val options = ConfigRetrieverOptions().addStore(envOptions)

    val retriever = ConfigRetriever.create(vertx, options)

    retriever.getConfig { configFuture ->
      if (configFuture.failed()) {
        log.error("Failed to get configuration", configFuture.cause())
        startFuture.fail(configFuture.cause())
        return@getConfig
      }

      val config = configFuture.result()

      log.trace(config.encodePrettily())

      // web

      val router = Router.router(vertx)

      router.route().handler(LoggerHandler.create())
      router.route().handler(BodyHandler.create())

      initVertx(vertx)

      // example1
      this.writeDb = pgPool(vertx, "WRITE", config)

      val ec = PgcEntityComponent(writeDb, "customer", CustomerJsonAware(), CustomerCommandAware(),
        EventBusUowPublisher(vertx, "example1"))

      WebEntityComponentImpl(ec, "customers", router).deployWebRoutes()

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

  override fun stop() {
    log.info("*** closing resources")
    writeDb.close()
    server.close()
  }

}

