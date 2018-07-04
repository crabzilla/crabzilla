package io.github.crabzilla.vertx.pgclient

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.vertx.ProjectionData
import io.github.crabzilla.vertx.configHandler
import io.reactiverse.pgclient.PgConnection
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.Tuple
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
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

    internal val log = org.slf4j.LoggerFactory.getLogger(PgClientEventProjectorIT::class.java)

    val customerId1 = CustomerId(1)
    val created1 = CustomerCreated(customerId1, "customer")
    val activated1 = CustomerActivated("a good reason", Instant.now())
    val deactivated1 = CustomerDeactivated("a good reason", Instant.now())

    val customerId2 = CustomerId(2)
    val created2 = CustomerCreated(customerId2, "customer")
    val activated2 = CustomerActivated("a good reason", Instant.now())
    val deactivated2 = CustomerDeactivated("a good reason", Instant.now())

    val customerId3 = CustomerId(3)
    val created3 = CustomerCreated(customerId3, "customer")
    val activated3 = CustomerActivated("a good reason", Instant.now())
    val deactivated3 = CustomerDeactivated("a good reason", Instant.now())

    val projectorFn: (pgConn: PgConnection, targetId: Int, event: DomainEvent, Future<Void>) -> Unit =
      { pgConn: PgConnection, targetId: Int, event: DomainEvent, future: Future<Void> ->

        log.info("event {} ", event)

        when (event) {
          is CustomerCreated -> {
            val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
            val tuple = Tuple.of(targetId, event.name, false)
            pgConn.pQuery(query, tuple, future)
          }
          is CustomerActivated -> {
            val query = "UPDATE customer_summary SET is_active = true WHERE id = $1"
            val tuple = Tuple.of(targetId)
            pgConn.pQuery(query, tuple, future)
          }
          is CustomerDeactivated -> {
            val query = "UPDATE customer_summary SET is_active = false WHERE id = $1"
            val tuple = Tuple.of(targetId)
            pgConn.pQuery(query, tuple, future)
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

        val component = DaggerPgClientComponent.builder()
          .pgClientModule(PgClientModule(vertx, config))
          .build()

        readDb = component.readDb()

        eventProjector = PgClientEventProjector(readDb)

        future.complete()

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

    readDb.query("DELETE FROM customer_summary", { ar1 ->

      if (ar1.failed()) {
        log.error("delete ", ar1.cause())
        tc.failNow(ar1.cause())
      }

      val projectionData = ProjectionData(UUID.randomUUID(), 2, customerId1.id, arrayListOf(created1, activated1))

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

      eventProjector.handle(arrayListOf(projectionData), projectorFn, future)

    })

  }

  @Test
  @DisplayName("can project a 3 events: created, activated and deactivated")
  fun a2(tc: VertxTestContext) {

    readDb.query("DELETE FROM customer_summary", { ar1 ->

      if (ar1.failed()) {
        log.error("delete ", ar1.cause())
        tc.failNow(ar1.cause())
      }

      val projectionData =
        ProjectionData(UUID.randomUUID(), 2, customerId1.id, arrayListOf(created1, activated1, deactivated1))

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

      eventProjector.handle(arrayListOf(projectionData), projectorFn, future)

    })

  }

  @Test
  @DisplayName("can project a 3 projectionData with 3 events each")
  fun a3(tc: VertxTestContext) {

    readDb.query("DELETE FROM customer_summary", { ar1 ->

      if (ar1.failed()) {
        log.error("delete ", ar1.cause())
        tc.failNow(ar1.cause())
      }

      val projectionData1 =
        ProjectionData(UUID.randomUUID(), 1, customerId1.id, arrayListOf(created1, activated1, deactivated1))

      val projectionData2 =
        ProjectionData(UUID.randomUUID(), 2, customerId2.id, arrayListOf(created2, activated2, deactivated2))

      val projectionData3 =
        ProjectionData(UUID.randomUUID(), 3, customerId3.id, arrayListOf(created3, activated3, deactivated3))

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

          assertThat(3).isEqualTo(result.size())
          assertThat(created1.name).isEqualTo(result.first().getString("name"))
          assertThat(result.first().getBoolean("is_active")).isFalse()

          tc.completeNow()

        })

      }

      eventProjector.handle(arrayListOf(projectionData1, projectionData2, projectionData3), projectorFn, future)

    })

  }

  @Test
  @DisplayName("on any any SQL error it must rollback all events projections")
  fun a4(tc: VertxTestContext) {

    val projectorFnErr: (pgConn: PgConnection, targetId: Int, event: DomainEvent, Future<Void>) -> Unit =
      { pgConn: PgConnection, targetId: Int, event: DomainEvent, future: Future<Void> ->

        log.info("event {} ", event)

        when (event) {
          is CustomerCreated -> {
            val query = "INSERT INTO XXXXXX  (id, name, is_active) VALUES ($1, $2, $3)"
            val tuple = Tuple.of(targetId, event.name, false)
            pgConn.pQuery(query, tuple, future)
          }
          is CustomerActivated -> {
            val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
            val tuple = Tuple.of(targetId, "name", false)
            pgConn.pQuery(query, tuple, future)
          }
          is CustomerDeactivated -> {
            val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
            val tuple = Tuple.of(targetId, "name", false)
            pgConn.pQuery(query, tuple, future)
          }
          else -> log.info("${event.javaClass.simpleName} does not have any event projector handler")
        }

        log.info("finished event {} ", event)
      }

    readDb.query("DELETE FROM customer_summary", { ar1 ->

      if (ar1.failed()) {
        log.error("delete ", ar1.cause())
        tc.failNow(ar1.cause())
      }

      val projectionData = ProjectionData(UUID.randomUUID(), 2, customerId1.id, arrayListOf(created1, activated1))

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

      eventProjector.handle(arrayListOf(projectionData), projectorFnErr, future)

    })

  }

}
