package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.initCrabzilla
import io.github.crabzilla.pgc.example1.CustomerSummaryProjector
import io.github.crabzilla.pgc.PgcComponent
import io.github.crabzilla.pgc.PgcProjectionComponent
import io.github.crabzilla.webpgc.WebPgcCmdHandlerComponent
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler

// Convenience method so you can run it in your IDE
fun main() {
  val vertx = Vertx.vertx()
  vertx.initCrabzilla()
  configRetriever(vertx, "example1.env").getConfig { gotConfig ->
    if (gotConfig.succeeded()) {
      val config = gotConfig.result()
      val deploymentOptions = DeploymentOptions().setConfig(config)
      vertx.deployVerticle(Example1Verticle::class.java, deploymentOptions) { deploy ->
        if (deploy.failed()) {
          deploy.cause().printStackTrace()
        }
      }
    } else {
      gotConfig.cause().printStackTrace()
    }
  }
}

fun configRetriever(vertx: Vertx, configFile: String): ConfigRetriever {
  val envOptions = ConfigStoreOptions()
    .setType("file")
    .setFormat("properties")
    .setConfig(JsonObject().put("path", configFile))
  val options = ConfigRetrieverOptions().addStore(envOptions)
  return ConfigRetriever.create(vertx, options)
}

// override val httpPort: Int, override val configFile: String = "./example1.env"
class Example1Verticle : AbstractVerticle() {

  lateinit var pgcComponent: PgcComponent

  companion object {
    init {
      System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
        SLF4JLogDelegateFactory::class.java.name)
      org.slf4j.LoggerFactory.getLogger(LoggerFactory::class.java)
    }
    internal var log = org.slf4j.LoggerFactory.getLogger(Example1Verticle::class.java)
  }

  override fun start(future: Future<Void>) {

    val config = config()
    log.info("*** config: \n" + config.encodePrettily())

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // example1
    pgcComponent = PgcComponent(vertx, config)
    val webCommandComponent = WebPgcCmdHandlerComponent(pgcComponent, router)
    webCommandComponent.addCommandHandler("customer", CustomerJsonAware(), CustomerCommandAware(), "customers")

    val projectionComponent = PgcProjectionComponent(pgcComponent)
    projectionComponent.addProjector("customer-summary", CustomerSummaryProjector())

    // read model routes
    router.get("/customers/:id").handler {
      it.response()
        .putHeader("Content-type", "application/json")
        .end(JsonObject().put("message", "TODO query read model").encode()) // TODO
    }

    // http server
    val httpPort = config.getInteger("HTTP_PORT")
    val server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
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

  override fun stop() {
    pgcComponent.writeDb.close()
    pgcComponent.readDb.close()
  }
}

