package io.github.crabzilla.web.example1

import io.github.crabzilla.*
import io.github.crabzilla.example1.CUSTOMER_CMD_HANDLER_FACTORY
import io.github.crabzilla.example1.CUSTOMER_CMD_VALIDATOR
import io.github.crabzilla.example1.CUSTOMER_SEED_VALUE
import io.github.crabzilla.example1.CUSTOMER_STATE_BUILDER
import io.github.crabzilla.example1.CustomerJson.CUSTOMER_CMD_FROM_JSON
import io.github.crabzilla.example1.CustomerJson.CUSTOMER_CMD_TO_JSON
import io.github.crabzilla.example1.CustomerJson.CUSTOMER_EVENT_FROM_JSON
import io.github.crabzilla.example1.CustomerJson.CUSTOMER_EVENT_TO_JSON
import io.github.crabzilla.example1.CustomerJson.CUSTOMER_FROM_JSON
import io.github.crabzilla.example1.CustomerJson.CUSTOMER_TO_JSON
import io.github.crabzilla.pgc.PgcEventProjector
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.pgc.PgcUowJournal
import io.github.crabzilla.pgc.PgcUowRepo
import io.github.crabzilla.pgc.example1.EXAMPLE1_PROJECTOR_HANDLER
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
import io.vertx.core.Handler
import io.vertx.core.Launcher
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import org.slf4j.LoggerFactory.getLogger
import java.util.*

// Convenience method so you can run it in your IDE
fun main() {
  Launcher.executeCommand("run", Example1Verticle::class.java.name)
}

class Example1Verticle(val httpPort: Int = 8081, val configFile: String = "./example1.env") : AbstractVerticle() {

  companion object {

    internal var server: HttpServer? = null
    internal var writeModelDb: PgPool? = null
    internal var readModelDb: PgPool? = null

    internal var log = getLogger(Example1Verticle::class.java)

    const val CUSTOMER_AGGREGATE_ROOT = "customer"
    const val EXAMPLE1_PROJECTION_ENDPOINT: String = "example1_projection_endpoint"

    const val COMMAND_NAME_PARAMETER = "commandName"
    const val COMMAND_ENTITY_ID_PARAMETER = "entityId"
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

      initVertx(vertx)

      val readDb = pgPool("READ", config)
      readModelDb = pgPool("READ", config)

      val writeDb = pgPool("WRITE", config)
      writeModelDb = writeDb

      // read model

      val eventProjector = PgcEventProjector(readDb, "customer summary")
      vertx.eventBus().consumer<ProjectionData>(EXAMPLE1_PROJECTION_ENDPOINT) { message ->
        log.info("received events: " + message.body())
        eventProjector.handle(message.body(), EXAMPLE1_PROJECTOR_HANDLER, Handler { result ->
          if (result.failed()) {
            log.error("Projection failed: " + result.cause().message)
          }
        })
      }

      // write model

      val uowRepository = PgcUowRepo(writeDb, CUSTOMER_CMD_FROM_JSON, CUSTOMER_EVENT_FROM_JSON)
      val uowJournal = PgcUowJournal(writeDb, CUSTOMER_CMD_TO_JSON, CUSTOMER_EVENT_TO_JSON)
      val snapshotRepo = PgcSnapshotRepo(CUSTOMER_AGGREGATE_ROOT, writeDb, CUSTOMER_SEED_VALUE, CUSTOMER_STATE_BUILDER,
        CUSTOMER_FROM_JSON, CUSTOMER_TO_JSON, CUSTOMER_EVENT_FROM_JSON)

      val commandVerticle = CommandHandlerVerticle(CommandHandlerEndpoint(CUSTOMER_AGGREGATE_ROOT),
        CUSTOMER_CMD_FROM_JSON, CUSTOMER_SEED_VALUE, CUSTOMER_STATE_BUILDER, CUSTOMER_CMD_HANDLER_FACTORY,
        CUSTOMER_CMD_VALIDATOR, uowJournal, snapshotRepo)

      vertx.deployVerticle(commandVerticle)

      // web

      val router = Router.router(vertx)

      router.route().handler(LoggerHandler.create())
      router.route().handler(BodyHandler.create())

      router.post("/customers/:entityId/commands/:commandName").handler {
        val commandMetadata = CommandMetadata(CUSTOMER_AGGREGATE_ROOT,
            it.pathParam(COMMAND_ENTITY_ID_PARAMETER).toInt(),
            it.pathParam(COMMAND_NAME_PARAMETER))
        postCommandHandler(it, commandMetadata, EXAMPLE1_PROJECTION_ENDPOINT)
      }

      router.get("/units-of-work/:unitOfWorkId").handler {
        val uowId = UUID.fromString(it.pathParam("unitOfWorkId"))
        getUowHandler(it, uowRepository, uowId) }

      router.get("/customers/:entityId").handler {

      }

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

/**
 *

abstract class CrabzillaDeployment(vertx: Vertx,
  readModelDb: PgPool,
  writeModelDb: PgPool,
  projectionEndpoint: String,
  persistSnapshots: Boolean,
  components: List<AggregateRootDeployment<Entity>>) {
  abstract fun projector(): PgcEventProjector
}

abstract class AggregateRootDeployment<E: Entity> {
  abstract fun uowJournal(): UnitOfWorkJournal
  abstract fun uowRepo() : UnitOfWorkRepository
  abstract fun snapshotRepo(): SnapshotRepository<E>
  abstract fun commandHandler(): CommandHandlerVerticle<E>
}

*/
