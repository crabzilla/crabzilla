package io.github.crabzilla.web

import io.github.crabzilla.Snapshot
import io.github.crabzilla.StateTransitionsTracker
import io.github.crabzilla.example1.*
import io.github.crabzilla.pgclient.PgClientEventProjector
import io.github.crabzilla.pgclient.PgClientUowRepo
import io.github.crabzilla.pgclient.example1.EXAMPLE1_PROJECTOR_HANDLER
import io.github.crabzilla.vertx.CommandVerticle
import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.ProjectionData
import io.github.crabzilla.vertx.VerticleRole.REST
import io.github.crabzilla.vertx.initVertx
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.circuitbreaker.CircuitBreaker
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
import net.jodah.expiringmap.ExpiringMap

// Convenience method so you can run it in your IDE
fun main(args: Array<String>) {
  Launcher.executeCommand("run", TestServer::class.java.name)
}

class TestServer(override val name: String = "testServer") : CrabzillaVerticle(name, REST) {

  companion object {

    private val PROJECTION_ENDPOINT: String = "example1_projection_endpoint"

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
      setupEventHandler(readDb)
      readDbHandle = readDb

      val writeDb = pgPool("WRITE", config)
      writeDbHandle = writeDb
      val uowRepository = PgClientUowRepo(writeDb)
      val customerCmdVerticle = customerCmdVerticle(uowRepository)

      vertx.deployVerticle(customerCmdVerticle)

      val router = Router.router(vertx)

      router.route().handler(LoggerHandler.create())
      router.route().handler(BodyHandler.create())

      router.post("/:resource/commands").handler { postCommandHandler(it, uowRepository, PROJECTION_ENDPOINT) }

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

  fun pgPool(id: String, config: JsonObject) : PgPool {
    val writeOptions = PgPoolOptions()
      .setPort(5432)
      .setHost(config.getString("${id}_DATABASE_HOST"))
      .setDatabase(config.getString("${id}_DATABASE_NAME"))
      .setUser(config.getString("${id}_DATABASE_USER"))
      .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
      .setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
    return PgClient.pool(vertx, writeOptions)
  }

  // related to example1

  fun customerCmdVerticle(uowRepository: PgClientUowRepo): CommandVerticle<Customer> {

    val seedValue = Customer(null, null, false, null, PojoService())
    val trackerFactory = { snapshot: Snapshot<Customer> -> StateTransitionsTracker(snapshot, CUSTOMER_STATE_BUILDER)}
    return CommandVerticle("Customer", seedValue, CUSTOMER_CMD_HANDLER, CUSTOMER_CMD_VALIDATOR,
      trackerFactory, uowRepository, ExpiringMap.create(), CircuitBreaker.create("cb1", vertx))

  }

  fun setupEventHandler(readDb: PgPool) {

    val eventProjector = PgClientEventProjector(readDb, "customer summary")

    vertx.eventBus().consumer<ProjectionData>(PROJECTION_ENDPOINT) { message ->

      println("received events: " + message.body())

      eventProjector.handle(message.body(), EXAMPLE1_PROJECTOR_HANDLER, Future.future())

    }

  }
}
