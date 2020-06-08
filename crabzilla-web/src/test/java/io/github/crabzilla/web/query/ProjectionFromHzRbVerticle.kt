package io.github.crabzilla.web.query

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.ringbuffer.Ringbuffer
import io.github.crabzilla.core.command.CrabzillaContext
import io.github.crabzilla.hazelcast.command.HzRingBufferPublisher
import io.github.crabzilla.hazelcast.query.HzProjectionRepo
import io.github.crabzilla.hazelcast.query.HzStreamConsumer
import io.github.crabzilla.pgc.command.PgcSnapshotRepo
import io.github.crabzilla.pgc.command.PgcUowJournal
import io.github.crabzilla.pgc.command.PgcUowRepo
import io.github.crabzilla.pgc.query.PgcUnitOfWorkProjector
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
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ProjectionFromHzRbVerticle : AbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(ProjectionFromHzRbVerticle::class.java)
    lateinit var hz: HazelcastInstance
  }

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

    createRingBuffer(CUSTOMER_ENTITY)
      .onFailure { err -> log.error("createRingBuffer", err); promise.fail(err) }
      .onSuccess { rb ->

        val uowJournal = PgcUowJournal(writeDb, example1Json, HzRingBufferPublisher(vertx, rb))
        val uowRepository = PgcUowRepo(writeDb, example1Json)
        val ctx = CrabzillaContext(example1Json, uowRepository, uowJournal)

        val cmdAware = CustomerCommandAware()
        val snapshotRepoDb = PgcSnapshotRepo(writeDb, example1Json, cmdAware) // TO write snapshots to db

        subRouteOf(router, ctx, WebResourceContext(cmdTypeMapOfCustomer, cmdAware, snapshotRepoDb))

        val uolProjector = PgcUnitOfWorkProjector(readDb, CUSTOMER_ENTITY, CUSTOMER_SUMMARY_STREAM,
          CustomerSummaryEventProjector())

        createMap(CUSTOMER_ENTITY, CUSTOMER_SUMMARY_STREAM)
          .onFailure { err -> log.error("createMap", err); promise.fail(err) }
          .onSuccess { map ->
            HzStreamConsumer(vertx, CUSTOMER_ENTITY, CUSTOMER_SUMMARY_STREAM, uolProjector,
              rb, example1Json, HzProjectionRepo(map)).start()
            // vertx http
            server = vertx.createHttpServer(HttpServerOptions().setPort(httpPort).setHost("0.0.0.0"))
            server.requestHandler(router).listen(listenHandler(promise))
          }
      }
  }

  private fun createRingBuffer(entityName: String): Future<Ringbuffer<String>> {
    val promise = Promise.promise<Ringbuffer<String>>()
    vertx.executeBlocking<Ringbuffer<String>>({ promise1 ->
      val result = hz.getRingbuffer<String>("$entityName")
      promise1.complete(result)
    }, { res ->
      if (res.failed()) {
        log.error("Failed to get ring buffer $entityName", res.cause())
        promise.fail(res.cause())
      } else {
        println("The result is: ${res.result()}")
        promise.complete(res.result())
      }
    })
    return promise.future()
  }

  private fun createMap(entityName: String, streamId: String): Future<AsyncMap<String, Long>> {
    val promise = Promise.promise<AsyncMap<String, Long>>()
    vertx.sharedData().getAsyncMap<String, Long>("$entityName-$streamId") { event1 ->
      if (event1.failed()) {
        log.error("Failed to get map $entityName-$streamId", event1.cause())
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      promise.complete(event1.result())
    }
    return promise.future()
  }
}
