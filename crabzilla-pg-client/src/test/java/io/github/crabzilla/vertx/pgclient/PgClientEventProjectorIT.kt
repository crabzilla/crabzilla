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

    val customerId = CustomerId(1)
    val created = CustomerCreated(customerId, "customer")
    val activated = CustomerActivated("a good reason", Instant.now())

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

      val projectionData = ProjectionData(UUID.randomUUID(), 2, customerId.id, arrayListOf(created, activated))

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

          println("result " + result.toList())

          assertThat(1).isEqualTo(result.size())
          assertThat(created.name).isEqualTo(result.first().getString("name"))
          assertThat(result.first().getBoolean("is_active")).isTrue()

          tc.completeNow()

        })

      }

      eventProjector.handle(arrayListOf(projectionData), projectorFn, future)

    })

  }

}
