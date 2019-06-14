package io.github.crabzilla.stack1

import io.github.crabzilla.initVertx
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory.getLogger

abstract class CrabzillaWebVerticle(open val httpPort: Int = 8081, open val configFile: String = "./config.env")
  : AbstractVerticle() {

  lateinit var server: HttpServer

  companion object {
    init {
      System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
        SLF4JLogDelegateFactory::class.java.name)
      getLogger(io.vertx.core.logging.LoggerFactory::class.java)
    }
    internal var log = getLogger(CrabzillaWebVerticle::class.java)
  }


  abstract fun startCrabzilla(config: JsonObject, router: Router)

  override fun start(startFuture: Future<Void>) {

    initVertx(vertx)

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

      val router = Router.router(vertx)

      startCrabzilla(config, router)

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

}

