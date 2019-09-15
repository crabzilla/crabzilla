package io.github.crabzilla.pgc

import io.github.crabzilla.framework.UnitOfWork
import io.github.crabzilla.internal.fromUnitOfWork
import io.github.crabzilla.pgc.example1.BadEventProjector
import io.github.crabzilla.pgc.example1.CustomerSummaryProjector
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.createActivateCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.createCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
import io.github.crabzilla.pgc.example1.Example1Fixture.deactivated1
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.*


@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcUowProjectorIT {

  private lateinit var vertx: Vertx
  private lateinit var readDb: PgPool
  private lateinit var uowProjector: PgcUowProjector

  companion object {

    // tip on how producers must fold events
    //      val groups = projectionDataList
    //        .flatMap { (_, uowId, targetId, events) -> events.map { Triple(uowId, targetId, it) }}
    //        .chunked(numberOfFutures)
    //

    internal val log = LoggerFactory.getLogger(PgcUowProjectorIT::class.java)


  }

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    val vertOption = VertxOptions()
    vertOption.blockedThreadCheckInterval = (1000 * 60 * 60).toLong() // to easier debug

    vertx = Vertx.vertx(vertOption)

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

      readDb = readModelPgPool(vertx, config)
      uowProjector = PgcUowProjector(readDb, "customer summary")
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
  @DisplayName("can project 1 event")
  fun a1(tc: VertxTestContext) {

    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1)))

    uowProjector.handle(fromUnitOfWork(1, uow), CustomerSummaryProjector()).future().setHandler(Handler { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@Handler
      }
      readDb.preparedQuery("SELECT * FROM customer_summary") { event2 ->
        if (event2.failed()) {
          tc.failNow(event2.cause())
          return@preparedQuery
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isFalse() }
        tc.completeNow()
      }
    })

  }

  @Test
  @DisplayName("can project 2 events: created and activated")
  fun a2(tc: VertxTestContext) {

    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), "Whatsoever",
      createActivateCmd1, 1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1)))

    uowProjector.handle(fromUnitOfWork(1, uow), CustomerSummaryProjector()).future().setHandler(Handler { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@Handler
      }
      readDb.preparedQuery("SELECT * FROM customer_summary") { event2 ->
        if (event2.failed()) {
          tc.failNow(event2.cause())
          return@preparedQuery
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isTrue() }
        tc.completeNow()
      }
    })

  }

  @Test
  @DisplayName("can project 3 events: created, activated and deactivated")
  fun a3(tc: VertxTestContext) {

    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1)))

    uowProjector.handle(fromUnitOfWork(1, uow), CustomerSummaryProjector()).future().setHandler(Handler { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@Handler
      }
      readDb.preparedQuery("SELECT * FROM customer_summary") { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@preparedQuery
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isFalse() }
        tc.completeNow()
      }
    })
  }

  @Test
  @DisplayName("can project 4 events: created, activated, deactivated, activated")
  fun a4(tc: VertxTestContext) {

    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1), Pair("CustomerActivated", activated1)))

    uowProjector.handle(fromUnitOfWork(1, uow), CustomerSummaryProjector()).future().setHandler(Handler { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@Handler
      }
      readDb.preparedQuery("SELECT * FROM customer_summary") { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@preparedQuery
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isTrue() }
        tc.completeNow()
      }
    })
  }

  @Test
  @DisplayName("can project 5 events: created, activated, deactivated, activated, deactivated")
  fun a5(tc: VertxTestContext) {

    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1)))

    uowProjector.handle(fromUnitOfWork(1, uow), CustomerSummaryProjector()).future().setHandler(Handler { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@Handler
      }
      readDb.preparedQuery("SELECT * FROM customer_summary") { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@preparedQuery
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isFalse() }
        tc.completeNow()
      }
    })
  }

  @Test
  @DisplayName("can project 6 events: created, activated, deactivated, activated, deactivated")
  fun a6(tc: VertxTestContext) {

    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1), Pair("CustomerActivated", activated1)))

    uowProjector.handle(fromUnitOfWork(1, uow), CustomerSummaryProjector()).future().setHandler(Handler { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@Handler
      }
      readDb.preparedQuery("SELECT * FROM customer_summary") { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@preparedQuery
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isTrue() }
        tc.completeNow()
      }
    })
  }

  @Test
  @DisplayName("cannot project more than 6 events within one transaction")
  fun a7(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), "Whatsoever", createCmd1,
      1, arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1), Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1), Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1),
      Pair("CustomerDeactivated", deactivated1)))

    uowProjector.handle(fromUnitOfWork(1, uow), CustomerSummaryProjector()).future().setHandler(Handler { result ->
      if (result.failed()) {
        tc.completeNow()
        return@Handler
      }
      tc.failNow(IllegalArgumentException())
    })
  }

  @Test
  @DisplayName("on any any SQL error it must rollback all events projections")
  fun a10(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", created1.customerId, UUID.randomUUID(), "Whatsoever",
      createCmd1, 1, arrayListOf(Pair("CustomerCreated", created1)))
    uowProjector.handle(fromUnitOfWork(1, uow), BadEventProjector()).future().setHandler(Handler { result ->
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
