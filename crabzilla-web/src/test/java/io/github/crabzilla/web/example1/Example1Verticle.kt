package io.github.crabzilla.web.example1

import io.github.crabzilla.example1.*
import io.github.crabzilla.initVertx
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.pgc.PgcUowJournal
import io.github.crabzilla.pgc.PgcUowRepo
import io.github.crabzilla.web.getUowHandler
import io.github.crabzilla.web.postCommandHandler
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
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
fun main(args: Array<String>) {
  Launcher.executeCommand("run", Example1Verticle::class.java.name)
}

class Example1Verticle(val httpPort: Int = 8081) : AbstractVerticle() {

  companion object {

    internal var server: HttpServer? = null
    internal var writeModelDb: PgPool? = null
    internal var readModelDb: PgPool? = null

    internal var log = getLogger(Example1Verticle::class.java)

  }

  override fun start(startFuture: Future<Void>) {

    log.info("*** starting using HTTP_PORT $httpPort")

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))

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
      readModelDb = readDb

      val writeDb = pgPool("WRITE", config)
      writeModelDb = writeDb

      // example1

      setupEventHandler(vertx, readDb)

      val uowRepository = PgcUowRepo(writeDb, CUSTOMER_CMD_FROM_JSON, CUSTOMER_EVENT_FROM_JSON)
      val uowJournal = PgcUowJournal(writeDb, CUSTOMER_CMD_TO_JSON, CUSTOMER_EVENT_TO_JSON)
      val snapshotRepo = PgcSnapshotRepo(writeDb, CUSTOMER_SEED_VALUE, CUSTOMER_STATE_BUILDER, CUSTOMER_FROM_JSON,
        CUSTOMER_EVENT_FROM_JSON)

      // command handlers verticles

      vertx.deployVerticle(customerCmdVerticle(uowJournal, snapshotRepo))

      // web

      val router = Router.router(vertx)

      router.route().handler(LoggerHandler.create())
      router.route().handler(BodyHandler.create())

      router.put("/:entityResource/:entityId/commands/:commandName").handler {
        postCommandHandler(it, EXAMPLE1_RESOURCE_TO_ENTITY, uowRepository, EXAMPLE1_PROJECTION_ENDPOINT) }

      router.get("/units-of-work/:unitOfWorkId").handler {
        getUowHandler(it, uowRepository) }

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
    writeModelDb?.close()
    readModelDb?.close()
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
