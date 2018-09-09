package io.github.crabzilla.pgclient

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.example1.*
import io.github.crabzilla.vertx.CrabzillaMainModule
import io.github.crabzilla.vertx.ProjectionData.Companion.fromUnitOfWork
import io.github.crabzilla.vertx.configHandler
import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
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
import java.time.Instant
import java.util.*


@ExtendWith(VertxExtension::class)
@DisplayName("PgClientEventProjector")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgClientEventProjectorIT {

  private lateinit var vertx: Vertx
  private lateinit var readDb: PgPool
  private lateinit var eventProjector: PgClientEventProjector

  companion object {

    // tip on how producers must fold events
    //      val groups = projectionDataList
    //        .flatMap { (_, uowSequence, targetId, events) -> events.map { Triple(uowSequence, targetId, it) }}
    //        .chunked(numberOfFutures)
    //

    internal val log = org.slf4j.LoggerFactory.getLogger(PgClientEventProjectorIT::class.java)

    val customerId1 = CustomerId(1)

    val command = CreateActivateCustomer(UUID.randomUUID(), customerId1, "customer1", "I need")

    val created1 = CustomerCreated(customerId1, "customer1")
    val activated1 = CustomerActivated("a good reason", Instant.now())
    val deactivated1 = CustomerDeactivated("a good reason", Instant.now())

    val customerId2 = CustomerId(2)
    val created2 = CustomerCreated(customerId2, "customer2")
    val activated2 = CustomerActivated("a good reason", Instant.now())
    val deactivated2 = CustomerDeactivated("a good reason", Instant.now())

    val customerId3 = CustomerId(3)
    val created3 = CustomerCreated(customerId3, "customer3")
    val activated3 = CustomerActivated("a good reason", Instant.now())
    val deactivated3 = CustomerDeactivated("a good reason", Instant.now())

    val uowSequence1 = 1

    val projectorHandler: (pgConn: PgConnection, targetId: Int, event: DomainEvent, Handler<AsyncResult<Void>>) -> Unit =
      { pgConn: PgConnection, targetId: Int, event: DomainEvent, handler: Handler<AsyncResult<Void>> ->

        log.info("event {} ", event)

        // TODO can I compose here? https://vertx.io/docs/vertx-core/java/#_sequential_composition
        val future: Future<Void> = Future.future<Void>()
        future.setHandler(handler)

        when (event) {
          is CustomerCreated -> {
            val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
            val tuple = Tuple.of(targetId, event.name, false)
            pgConn.runPreparedQuery(query, tuple, future)
          }
          is CustomerActivated -> {
            val query = "UPDATE customer_summary SET is_active = true WHERE id = $1"
            val tuple = Tuple.of(targetId)
            pgConn.runPreparedQuery(query, tuple, future)
          }
          is CustomerDeactivated -> {
            val query = "UPDATE customer_summary SET is_active = false WHERE id = $1"
            val tuple = Tuple.of(targetId)
            pgConn.runPreparedQuery(query, tuple, future)
          }
          else -> log.info("${event.javaClass.simpleName} does not have any event projector handler")
        }

        log.info("finished event {} ", event)
      }

  }

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    val options = VertxOptions()
    options.blockedThreadCheckInterval = (1000 * 60 * 60).toLong() // to easier debug

    vertx = Vertx.vertx(options)

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))

    configHandler(vertx, envOptions, { config ->

      vertx.executeBlocking<Any>({ future ->

        val component = DaggerPgClientTestComponent.builder()
          .crabzillaMainModule(CrabzillaMainModule(vertx, config))
          .pgClientModule(PgClientModule())
          .build()

        readDb = component.readDb()

        eventProjector = PgClientEventProjector(readDb, "customer summary")

        readDb.query("DELETE FROM customer_summary", { ar1 ->

          if (ar1.failed()) {
            log.error("delete ", ar1.cause())
            tc.failNow(ar1.cause())
          }

          future.complete()

        })

      }, { res ->

        if (res.failed()) {
          tc.failNow(res.cause())
        }
        tc.completeNow()

      })

    }, {

      readDb.close()

    })

  }

  @Test
  @DisplayName("can project a couple of events: created and activated")
  fun a1(tc: VertxTestContext) {

    val future = Future.future<Boolean>()

    future.setHandler { ar2 ->

      if (ar2.failed()) {
        log.error("future", ar2.cause())
        tc.failNow(ar2.cause())
      }

      val ok = ar2.result()

      assertThat(ok).isTrue()

      readDb.preparedQuery("SELECT * FROM customer_summary", { ar3 ->

        if (ar3.failed()) {
          log.error("select", ar3.cause())
          tc.failNow(ar3.cause())
        }

        val result = ar3.result()

        assertThat(1).isEqualTo(result.size())
        assertThat(created1.name).isEqualTo(result.first().getString("name"))
        assertThat(result.first().getBoolean("is_active")).isTrue()

        tc.completeNow()

      })

    }

    val uow = UnitOfWork(UUID.randomUUID(), command, 1, arrayListOf(created1, activated1))

    eventProjector.handle(fromUnitOfWork(uowSequence1, uow), projectorHandler, future)

  }

  @Test
  @DisplayName("can project 3 events: created, activated and deactivated")
  fun a2(tc: VertxTestContext) {

    val future = Future.future<Boolean>()

    future.setHandler { ar2 ->

      if (ar2.failed()) {
        log.error("future", ar2.cause())
        tc.failNow(ar2.cause())
      }

      val ok = ar2.result()

      assertThat(ok).isTrue()

      readDb.preparedQuery("SELECT * FROM customer_summary", { ar3 ->

        if (ar3.failed()) {
          log.error("select", ar3.cause())
          tc.failNow(ar3.cause())
        }

        val result = ar3.result()

        assertThat(1).isEqualTo(result.size())
        assertThat(created1.name).isEqualTo(result.first().getString("name"))
        assertThat(result.first().getBoolean("is_active")).isFalse()

        tc.completeNow()

      })

    }

    val uow = UnitOfWork(UUID.randomUUID(), command, 1, arrayListOf(created1, activated1, deactivated1))

    eventProjector.handle(fromUnitOfWork(uowSequence1, uow), projectorHandler, future)

  }

  @Test
  @DisplayName("cannot project more than 6 events within one transaction")
  fun a4(tc: VertxTestContext) {


    val future = Future.future<Boolean>()

    future.setHandler { ar2 ->

      if (ar2.failed()) {

        readDb.preparedQuery("SELECT * FROM customer_summary", { ar3 ->

          if (ar3.failed()) {
            log.error("select", ar3.cause())
            tc.failNow(ar3.cause())
          }

          val result = ar3.result()

          assertThat(0).isEqualTo(result.size())

          tc.completeNow()

        })

      }

    }

    val uow = UnitOfWork(UUID.randomUUID(), command, 1, arrayListOf(created1, activated1, deactivated1,
      created2, activated2, deactivated2, created1))

    eventProjector.handle(fromUnitOfWork(uowSequence1, uow), projectorHandler, future)

  }

  @Test
  @DisplayName("on any any SQL error it must rollback all events projections")
  fun a5(tc: VertxTestContext) {

    val projectorToFail: (pgConn: PgConnection, targetId: Int, event: DomainEvent, Handler<AsyncResult<Void>>) -> Unit =
      { pgConn: PgConnection, targetId: Int, event: DomainEvent, handler: Handler<AsyncResult<Void>> ->

        log.info("event {} ", event)

        // TODO can I use this here? https://vertx.io/docs/vertx-core/java/#_sequential_composition
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
          else -> log.info("${event.javaClass.simpleName} does not have any event projector handler")
        }

        log.info("finished event {} ", event)
      }


    val future2 = Future.future<Boolean>()

    future2.setHandler { ar2 ->

      if (ar2.succeeded()) {
        tc.failNow(IllegalStateException("should fail"))
      }

      readDb.preparedQuery("SELECT * FROM customer_summary", { ar3 ->

        if (ar3.failed()) {
          log.error("select", ar3.cause())
          tc.failNow(ar3.cause())
        }

        val result = ar3.result()

        assertThat(0).isEqualTo(result.size())

        tc.completeNow()

      })

    }

    val uow = UnitOfWork(UUID.randomUUID(), command, 1, arrayListOf(created1, activated1))

    eventProjector.handle(fromUnitOfWork(uowSequence1, uow), projectorToFail, future2)

  }

}
