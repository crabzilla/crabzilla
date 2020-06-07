package io.github.crabzilla.web.query

import io.github.crabzilla.core.command.CrabzillaContext
import io.github.crabzilla.pgc.PgcStreamProjector
import io.github.crabzilla.pgc.command.PgcSnapshotRepo
import io.github.crabzilla.pgc.command.PgcUowJournal
import io.github.crabzilla.pgc.command.PgcUowJournal.FullPayloadPublisher
import io.github.crabzilla.pgc.command.PgcUowRepo
import io.github.crabzilla.pgc.query.PgcProjectionsRepo
import io.github.crabzilla.pgc.query.PgcReadContext
import io.github.crabzilla.pgc.query.startStreamProjectionConsumer
import io.github.crabzilla.pgc.query.startStreamProjectionDbPoolingProducer
import io.github.crabzilla.web.boilerplate.HttpSupport.listenHandler
import io.github.crabzilla.web.boilerplate.PgClientSupport.readModelPgPool
import io.github.crabzilla.web.boilerplate.PgClientSupport.writeModelPgPool
import io.github.crabzilla.web.command.WebResourceContext
import io.github.crabzilla.web.command.WebResourceContext.Companion.subRouteOf
import io.github.crabzilla.web.example1.CustomerCommandAware
import io.github.crabzilla.web.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.web.example1.Example1Fixture.CUSTOMER_SUMMARY_STREAM
import io.github.crabzilla.web.example1.customerModule
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json

class ProjectionFromDbVerticle : AbstractVerticle() {

  val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }
  val writeDb: PgPool by lazy { writeModelPgPool(vertx, config()) }

  val httpPort: Int by lazy { config().getInteger("HTTP_PORT") }
  lateinit var server: HttpServer

  override fun start(promise: Promise<Void>) {

    val router = Router.router(vertx)
    router.route().handler(LoggerHandler.create())
    router.route().handler(BodyHandler.create())

    // query routes
    router.get("/customers/:id").handler { rc -> customersQueryHandler(rc, readDb) }

    // command routes
    val example1Json = Json(context = customerModule)
    val uowJournal = PgcUowJournal(writeDb, example1Json, FullPayloadPublisher(vertx))
    val uowRepository = PgcUowRepo(writeDb, example1Json)
    val ctx = CrabzillaContext(example1Json, uowRepository, uowJournal)

    val cmdAware = CustomerCommandAware()
    val snapshotRepoDb = PgcSnapshotRepo(writeDb, example1Json, cmdAware) // TO write snapshots to db
    // val snapshotRepo = InMemorySnapshotRepository(vertx.sharedData(), example1Json, cmdAware)

    subRouteOf(router, ctx, WebResourceContext(cmdTypeMapOfCustomer, cmdAware, snapshotRepoDb))

    // projection consumer
    val readContext = PgcReadContext(vertx, example1Json, readDb)
    startStreamProjectionConsumer(CUSTOMER_ENTITY, CUSTOMER_SUMMARY_STREAM,
      readContext, CustomerSummaryEventProjector())

    // projection producer
    val streamProjector = PgcStreamProjector(vertx, writeDb, CUSTOMER_ENTITY, CUSTOMER_SUMMARY_STREAM)
    startStreamProjectionDbPoolingProducer(vertx, PgcProjectionsRepo(readDb), streamProjector)
      .onFailure { err -> promise.fail(err) }
      .onSuccess {
        // vertx http
        server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
        server.requestHandler(router).listen(listenHandler(promise))
      }
  }
}
