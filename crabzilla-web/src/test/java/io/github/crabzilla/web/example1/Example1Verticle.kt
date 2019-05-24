package io.github.crabzilla.web.example1

import io.github.crabzilla.CommandMetadata
import io.github.crabzilla.Crabzilla
import io.github.crabzilla.UnitOfWorkEvents
import io.github.crabzilla.pgc.PgcUowProjector
import io.github.crabzilla.pgc.example1.Example1EventProjector
import io.github.crabzilla.pgc.example1.Example1Fixture.customerDeploymentFn
import io.github.crabzilla.web.ContentTypes.ENTITY_TRACKING
import io.github.crabzilla.web.ContentTypes.ENTITY_WRITE_MODEL
import io.github.crabzilla.web.entityTrackingHandler
import io.github.crabzilla.web.entityWriteModelHandler
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

// Convenience method so you can run it in your IDE
fun main() {
  Launcher.executeCommand("run", Example1Verticle::class.java.name)
}

class Example1Verticle(val httpPort: Int = 8081, val configFile: String = "./example1.env") : AbstractVerticle() {

  lateinit var server: HttpServer
  lateinit var writeModelDb: PgPool
  lateinit var readModelDb: PgPool

  companion object {

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

      Crabzilla.initVertx(vertx)

      val readDb = pgPool("READ", config)
      readModelDb = pgPool("READ", config)

      val writeDb = pgPool("WRITE", config)
      writeModelDb = writeDb

      // read model

      val eventProjector = PgcUowProjector(readDb, "customer summary")
      vertx.eventBus().consumer<UnitOfWorkEvents>(EXAMPLE1_PROJECTION_ENDPOINT) { message ->
        log.info("received events: " + message.body())
        eventProjector.handle(message.body(), Example1EventProjector(), Handler { result ->
          if (result.failed()) {
            log.error("Projection failed: " + result.cause().message)
          }
        })
      }

      // write model

      val customerDeployment = customerDeploymentFn(writeDb)

      vertx.deployVerticle(customerDeployment.cmdHandlerVerticle.value)

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
        val uowId = it.pathParam("unitOfWorkId").toLong()
        println("retrieving uow $uowId")
        getUowHandler(it, customerDeployment.uowRepo.value, uowId)
      }

      router.get("/customers/:entityId").handler {
        val customerId = it.pathParam(COMMAND_ENTITY_ID_PARAMETER).toInt()
        println(it.request().getHeader("accept"))
        when (it.request().getHeader("accept")) {
          ENTITY_TRACKING -> entityTrackingHandler(it, customerId, customerDeployment.uowRepo.value,
            customerDeployment.snapshotRepo.value) { customer -> customerDeployment.jsonFn.toJson(customer) }
          ENTITY_WRITE_MODEL -> entityWriteModelHandler(it, customerId, customerDeployment.snapshotRepo.value)
            { customer -> customerDeployment.jsonFn.toJson(customer) }
          else -> {
            readDb.preparedQuery("select * from customer_summary") { event1 ->
              println("*** read model: " + event1.result().toString())
            }
          }
        } // TODO plug read model handler as default

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

abstract class CrabzillaDeployment(
  vertx: Vertx,
  readModelDb: PgPool,
  writeModelDb: PgPool,
  projectionEndpoint: String,
  components: List<AggregateRootDeployment<Entity>>) {
  abstract fun projector(): PgcEventProjector
}

abstract class AggregateRootDeployment<E: Entity> {
  abstract fun name(): String
  abstract fun uowJournal(): UnitOfWorkJournal
  abstract fun uowRepo() : UnitOfWorkRepository
  abstract fun snapshotRepo(): SnapshotRepository<E>
  abstract fun commandHandler(): CommandHandlerVerticle<E>
}

*/
