package io.github.crabzilla.web

import io.github.crabzilla.pgclient.PgClientUowRepo
import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.VerticleRole.REST
import io.github.crabzilla.vertx.initVertx
import io.github.crabzilla.web.example1.EXAMPLE1_PROJECTION_ENDPOINT
import io.github.crabzilla.web.example1.customerCmdVerticle
import io.github.crabzilla.web.example1.setupEventHandler
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.Launcher
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory.getLogger
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler

// Convenience method so you can run it in your IDE
fun main(args: Array<String>) {
  Launcher.executeCommand("run", TestServer::class.java.name)
}

class TestServer(override val name: String = "testServer") : CrabzillaVerticle(name, REST) {

  companion object {

    internal var httpPort: Int = 8081
    internal var server: HttpServer? = null
    internal var writeDbHandle: PgPool? = null
    internal var readDbHandle: PgPool? = null

    internal var log = getLogger(TestServer::class.java)

  }

  override fun start(startFuture: Future<Void>) {

    log.info("*** starting using HTTP_PORT $httpPort")

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "example1.env"))

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

      initVertx(vertx)

      val readDb = pgPool("READ", config)
      readDbHandle = readDb

      val writeDb = pgPool("WRITE", config)
      writeDbHandle = writeDb

      // example1

      setupEventHandler(vertx, readDb)
      val uowRepository = PgClientUowRepo(writeDb)
      val customerCmdVerticle = customerCmdVerticle(vertx, uowRepository)
      vertx.deployVerticle(customerCmdVerticle)

      // web

      val router = Router.router(vertx)

      router.route().handler(LoggerHandler.create())
      router.route().handler(BodyHandler.create())

      router.post("/:resource/commands").handler { postCommandHandler(it, uowRepository, EXAMPLE1_PROJECTION_ENDPOINT) }
      router.get("/:resource/commands/:cmdID").handler { getUowByCmdIdHandler(it, uowRepository) }

      server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))

      server!!.requestHandler(router).listen { startedFuture ->
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
    writeDbHandle?.close()
    readDbHandle?.close()
    server?.close()
  }

  private fun pgPool(id: String, config: JsonObject) : PgPool {
    val writeOptions = PgPoolOptions()
      .setPort(5432)
      .setHost(config.getString("${id}_DATABASE_HOST"))
      .setDatabase(config.getString("${id}_DATABASE_NAME"))
      .setUser(config.getString("${id}_DATABASE_USER"))
      .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
      .setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
    return PgClient.pool(vertx, writeOptions)
  }

}
