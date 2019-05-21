package io.github.crabzilla.pgc

import io.github.crabzilla.Crabzilla
import io.github.crabzilla.DomainEvent
import io.github.crabzilla.ProjectionData.Companion.fromUnitOfWork
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.example1.CustomerActivated
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerDeactivated
import io.github.crabzilla.pgc.example1.EXAMPLE1_PROJECTOR_HANDLER
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.createCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
import io.github.crabzilla.pgc.example1.Example1Fixture.deactivated1
import io.reactiverse.pgclient.*
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.*
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.*
import io.github.crabzilla.UnitOfWork.Companion as UnitOfWork1


@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcEventProjectorIT {

  private lateinit var vertx: Vertx
  private lateinit var readDb: PgPool
  private lateinit var eventProjector: PgcEventProjector

  companion object {

    // tip on how producers must fold events
    //      val groups = projectionDataList
    //        .flatMap { (_, uowSequence, targetId, events) -> events.map { Triple(uowSequence, targetId, it) }}
    //        .chunked(numberOfFutures)
    //

    internal val log = LoggerFactory.getLogger(PgcEventProjectorIT::class.java)


  }

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    val vertOption = VertxOptions()
    vertOption.blockedThreadCheckInterval = (1000 * 60 * 60).toLong() // to easier debug

    vertx = Vertx.vertx(vertOption)

    Crabzilla.initVertx(vertx)

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))

    val options = ConfigRetrieverOptions().addStore(envOptions)

    val retriever = ConfigRetriever.create(vertx, options)

    retriever.getConfig(Handler { configFuture ->

      if (configFuture.failed()) {
        println("Failed to get configuration")
        tc.failNow(configFuture.cause())
        return@Handler
      }

      val config = configFuture.result()

      // println(config.encodePrettily())

      val options = PgPoolOptions()
        .setPort(5432)
        .setHost(config.getString("READ_DATABASE_HOST"))
        .setDatabase(config.getString("READ_DATABASE_NAME"))
        .setUser(config.getString("READ_DATABASE_USER"))
        .setPassword(config.getString("READ_DATABASE_PASSWORD"))
        .setMaxSize(config.getInteger("READ_DATABASE_POOL_MAX_SIZE"))

      readDb = PgClient.pool(vertx, options)

      eventProjector = PgcEventProjector(readDb, "customer summary")

      readDb.query("DELETE FROM customer_summary") { deleted ->

        if (deleted.failed()) {
          log.error("delete ", deleted.cause())
          tc.failNow(deleted.cause())
          return@query
        }

        tc.completeNow()

      }

    })

  }

  @Test
  @DisplayName("can project a couple of events: created and activated")
  fun a1(tc: VertxTestContext) {

    val uow = UnitOfWork(UUID.randomUUID(), "Customer", customerId1.value, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1)))

    eventProjector.handle(fromUnitOfWork(1, uow), EXAMPLE1_PROJECTOR_HANDLER, Handler { result ->

      if (result.failed()) {
        tc.failNow(result.cause())
        return@Handler
      }

      readDb.preparedQuery("SELECT * FROM customer_summary") { ar3 ->

        if (ar3.failed()) {
          tc.failNow(ar3.cause())
          return@preparedQuery
        }

        val result = ar3.result()

        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isTrue() }

        tc.completeNow()

      }
    })

  }

  @Test
  @DisplayName("can project 3 events: created, activated and deactivated")
  fun a2(tc: VertxTestContext) {

    val uow = UnitOfWork(UUID.randomUUID(), "Customer", customerId1.value, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1)))

    eventProjector.handle(fromUnitOfWork(1, uow), EXAMPLE1_PROJECTOR_HANDLER, Handler { result ->

      if (result.failed()) {
        tc.failNow(result.cause())
        return@Handler
      }

      readDb.preparedQuery("SELECT * FROM customer_summary") { ar3 ->

        if (ar3.failed()) {
          log.error("select", ar3.cause())
          tc.failNow(ar3.cause())
          return@preparedQuery
        }

        val result = ar3.result()

        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isFalse() }

        tc.completeNow()

      }
    })


  }

  @Test
  @DisplayName("cannot project more than 6 events within one transaction")
  fun a4(tc: VertxTestContext) {

    val uow = UnitOfWork(UUID.randomUUID(), "Customer", customerId1.value, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1),  Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1), Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1)))

    eventProjector.handle(fromUnitOfWork(1, uow), EXAMPLE1_PROJECTOR_HANDLER, Handler { result ->
      if (result.failed()) {
        tc.completeNow()
        return@Handler
      }
      tc.failNow(IllegalArgumentException())
    })

  }

  @Test
  @DisplayName("on any any SQL error it must rollback all events projections")
  fun a5(tc: VertxTestContext) {

    val projectorToFail: ProjectorHandler =
      { pgConn: PgTransaction, targetId: Int, event: DomainEvent, handler: Handler<AsyncResult<Void>> ->

        log.info("event {} ", event)

        val future: Future<Void> = Future.future<Void>()

        future.setHandler(handler)

        when (event) {
          is CustomerCreated -> {
              val query = "INSERT INTO XXXXXX (id, name, is_active) VALUES ($1, $2, $3)"
              val tuple = Tuple.of(targetId, event.name, false)
              pgConn.runPreparedQuery(query, tuple, future)
          }
          is CustomerActivated -> {
            val query = "UPDATE XXX SET is_active = true WHERE id = $1"
            val tuple = Tuple.of(targetId)
            pgConn.runPreparedQuery(query, tuple, future)
          }
          is CustomerDeactivated -> {
            val query = "UPDATE XX SET is_active = false WHERE id = $1"
            val tuple = Tuple.of(targetId)
            pgConn.runPreparedQuery(query, tuple, future)
          }
          else -> {
            future.fail("${event.javaClass.simpleName} does not have any event projector handler")
            log.info("${event.javaClass.simpleName} does not have any event projector handler")
          }
        }

        log.info("finished event {} ", event)
      }


    val uow = UnitOfWork(UUID.randomUUID(), "Customer", created1.customerId.value, UUID.randomUUID(), "Whatsoever",
                                createCmd1, 1, arrayListOf(Pair("CustomerCreated", created1)))

    eventProjector.handle(fromUnitOfWork(1, uow), projectorToFail, Handler { result ->

      if (result.succeeded()) {
        tc.failNow(result.cause())
        return@Handler
      }

      readDb.preparedQuery("SELECT * FROM customer_summary") { ar3 ->

        if (ar3.failed()) {
          log.error("select", ar3.cause())
          tc.failNow(ar3.cause())
          return@preparedQuery
        }

        val pgRowSet = ar3.result()

        tc.verify { assertThat(0).isEqualTo(pgRowSet.size()) }

        tc.completeNow()

      }

    })

  }

}
