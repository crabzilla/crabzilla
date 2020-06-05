package io.github.crabzilla.web.example1

import io.github.crabzilla.core.command.CrabzillaContext
import io.github.crabzilla.pgc.command.PgcSnapshotRepo
import io.github.crabzilla.pgc.command.PgcStreamProjector
import io.github.crabzilla.pgc.command.PgcUowJournal
import io.github.crabzilla.pgc.command.PgcUowJournal.EmptyPayloadPublisher
import io.github.crabzilla.pgc.command.PgcUowRepo
import io.github.crabzilla.pgc.query.PgcProjectionRepo
import io.github.crabzilla.pgc.query.PgcUowProjector
import io.github.crabzilla.pgc.query.startProjection
import io.github.crabzilla.web.boilerplate.listenHandler
import io.github.crabzilla.web.boilerplate.readModelPgPool
import io.github.crabzilla.web.boilerplate.writeModelPgPool
import io.github.crabzilla.web.command.WebResourceContext
import io.github.crabzilla.web.command.WebResourcesRegistry
import io.github.crabzilla.web.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.web.example1.Example1Fixture.CUSTOMER_SUMMARY_STREAM
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json

class CustomerVerticle2 : AbstractVerticle() {

  val httpPort: Int by lazy { config().getInteger("HTTP_PORT") }
  val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }
  val writeDb: PgPool by lazy { writeModelPgPool(vertx, config()) }
  lateinit var server: HttpServer

  override fun start(promise: Promise<Void>) {

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    val example1Json = Json(context = customerModule)
    val uowJournal = PgcUowJournal(writeDb, example1Json, EmptyPayloadPublisher(vertx))
    val uowRepository = PgcUowRepo(writeDb, example1Json)
    val ctx = CrabzillaContext(example1Json, uowRepository, uowJournal)

    // web command routes
    val cmdTypeMapOfCustomer = mapOf(
      Pair("create", CreateCustomer::class.qualifiedName as String),
      Pair("activate", ActivateCustomer::class.qualifiedName as String),
      Pair("deactivate", DeactivateCustomer::class.qualifiedName as String),
      Pair("create-activate", CreateActivateCustomer::class.qualifiedName as String))

    val cmdAware = CustomerCommandAware()
    val snapshotRepoDb = PgcSnapshotRepo(writeDb, example1Json, cmdAware) // TO write snapshots to db
    // val snapshotRepo = InMemorySnapshotRepository(vertx.sharedData(), example1Json, cmdAware)

    WebResourcesRegistry()
      .add(router, ctx, WebResourceContext(cmdTypeMapOfCustomer, cmdAware, snapshotRepoDb))

    // projections
    val uowProjector = PgcUowProjector(readDb, CUSTOMER_ENTITY, CUSTOMER_SUMMARY_STREAM, CustomerSummaryProjector())
    val streamProjector = PgcStreamProjector(writeDb, example1Json, uowProjector)
    startProjection(vertx, PgcProjectionRepo(readDb), streamProjector)
      .onFailure { err -> promise.fail(err) }
      .onSuccess {
        // read model routes
        router.get("/customers/:id").handler(::customersQueryHandler)
        // vertx http
        server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
        server.requestHandler(router).listen(listenHandler(promise))
      }
  }

  private fun customersQueryHandler(rc: RoutingContext) {
    val sql = """SELECT id, name, is_active FROM customer_summary where id = $1""".trimMargin()
    val id = rc.request().getParam("id").toInt()
    readDb.preparedQuery(sql).execute(Tuple.of(id)) { event ->
      if (event.failed()) {
        rc.response()
          .putHeader("Content-type", "application/json")
          .setStatusCode(500)
          .setStatusMessage(event.cause().message)
          .end()
      }
      val set = event.result()
      val array = JsonArray()
      for (row in set) {
        val jo =
          JsonObject().put("id", row.getInteger(0)).put("name", row.getString(1)).put("is_active", row.getBoolean(2))
        array.add(jo)
      }
      rc.response()
        .putHeader("Content-type", "application/json")
        .end(array.getJsonObject(0).encode())
    }
  }
}
