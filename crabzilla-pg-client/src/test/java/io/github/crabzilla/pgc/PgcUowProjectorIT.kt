package io.github.crabzilla.pgc

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.internal.UnitOfWorkEvents
import io.github.crabzilla.pgc.example1.BadEventProjector
import io.github.crabzilla.pgc.example1.CustomerActivated
import io.github.crabzilla.pgc.example1.CustomerCreated
import io.github.crabzilla.pgc.example1.CustomerDeactivated
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.createCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
import io.github.crabzilla.pgc.example1.Example1Fixture.deactivated1
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory

@ExtendWith(VertxExtension::class)
class PgcUowProjectorIT {

  private lateinit var vertx: Vertx
  private lateinit var readDb: PgPool
  private lateinit var uowProjector: PgcUowProjector

  companion object {
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
      readDb.query("DELETE FROM customer_summary").execute { deleted ->
        if (deleted.failed()) {
          log.error("delete ", deleted.cause())
          tc.failNow(deleted.cause())
          return@execute
        }
        tc.completeNow()
      }
    })
  }

  @Test
  @DisplayName("can project 1 event")
  fun a1(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), createCmd1, 1, listOf(created1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), CustomerProjector()).onComplete { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@onComplete
      }
      readDb.preparedQuery("SELECT * FROM customer_summary").execute { event2 ->
        if (event2.failed()) {
          tc.failNow(event2.cause())
          return@execute
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isFalse() }
        tc.completeNow()
      }
    }
  }

  @Test
  @DisplayName("can project 2 events: created and activated")
  fun a2(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), createCmd1, 1, listOf(created1, activated1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), CustomerProjector()).onComplete { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@onComplete
      }
      readDb.preparedQuery("SELECT * FROM customer_summary").execute { event2 ->
        if (event2.failed()) {
          tc.failNow(event2.cause())
          return@execute
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isTrue() }
        tc.completeNow()
      }
    }
  }

  @Test
  @DisplayName("can project 3 events: created, activated and deactivated")
  fun a3(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), createCmd1, 1,
      listOf(created1, activated1, deactivated1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), CustomerProjector()).onComplete { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@onComplete
      }
      readDb.preparedQuery("SELECT * FROM customer_summary").execute { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@execute
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isFalse() }
        tc.completeNow()
      }
    }
  }

  @Test
  @DisplayName("can project 4 events: created, activated, deactivated, activated")
  fun a4(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), createCmd1, 1,
      listOf(created1, activated1, deactivated1, activated1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), CustomerProjector()).onComplete { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@onComplete
      }
      readDb.preparedQuery("SELECT * FROM customer_summary").execute { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@execute
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isTrue() }
        tc.completeNow()
      }
    }
  }

  @Test
  @DisplayName("can project 5 events: created, activated, deactivated, activated, deactivated")
  fun a5(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), createCmd1, 1,
      listOf(created1, activated1, deactivated1, activated1, deactivated1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), CustomerProjector()).onComplete { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@onComplete
      }
      readDb.preparedQuery("SELECT * FROM customer_summary").execute { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@execute
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isFalse() }
        tc.completeNow()
      }
    }
  }

  @Test
  @DisplayName("can project 6 events: created, activated, deactivated, activated, deactivated")
  fun a6(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), createCmd1, 1,
      listOf(created1, activated1, deactivated1, activated1, deactivated1, activated1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), CustomerProjector()).onComplete { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@onComplete
      }
      readDb.preparedQuery("SELECT * FROM customer_summary").execute { event2 ->
        if (event2.failed()) {
          log.error("select", event2.cause())
          tc.failNow(event2.cause())
          return@execute
        }
        val result = event2.result()
        tc.verify { assertThat(1).isEqualTo(result.size()) }
        tc.verify { assertThat(created1.name).isEqualTo(result.first().getString("name")) }
        tc.verify { assertThat(result.first().getBoolean("is_active")).isTrue() }
        tc.completeNow()
      }
    }
  }

  @Test
  @DisplayName("cannot project more than 6 events within one transaction")
  fun a7(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", customerId1, UUID.randomUUID(), createCmd1, 1,
      listOf(created1, activated1, deactivated1, activated1, deactivated1, activated1, deactivated1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), CustomerProjector()).onComplete { result ->
      if (result.failed()) {
        tc.completeNow()
        return@onComplete
      }
      tc.failNow(IllegalArgumentException())
    }
  }

  @Test
  @DisplayName("on any any SQL error it must rollback all events projections")
  fun a10(tc: VertxTestContext) {
    val uow = UnitOfWork("Customer", created1.customerId, UUID.randomUUID(), createCmd1, 1, listOf(created1))
    uowProjector.handle(UnitOfWorkEvents(1, uow.entityId, uow.events), BadEventProjector()).onComplete { result ->
      if (result.succeeded()) {
        tc.failNow(result.cause())
        return@onComplete
      }
      readDb.preparedQuery("SELECT * FROM customer_summary").execute { ar3 ->
        if (ar3.failed()) {
          log.error("select", ar3.cause())
          tc.failNow(ar3.cause())
          return@execute
        }
        val pgRowSet = ar3.result()
        tc.verify { assertThat(0).isEqualTo(pgRowSet.size()) }
        tc.completeNow()
      }
    }
  }
}

class CustomerProjector : PgcEventProjector {

  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
    return when (event) {
      is CustomerCreated -> {
        val query = "INSERT INTO customer_summary (id, name, is_active) VALUES ($1, $2, $3)"
        val tuple = Tuple.of(targetId, event.name, false)
        executePreparedQuery(pgTx, query, tuple)
      }
      is CustomerActivated -> {
        val query = "UPDATE customer_summary SET is_active = true WHERE id = $1"
        val tuple = Tuple.of(targetId)
        executePreparedQuery(pgTx, query, tuple)
      }
      is CustomerDeactivated -> {
        val query = "UPDATE customer_summary SET is_active = false WHERE id = $1"
        val tuple = Tuple.of(targetId)
        executePreparedQuery(pgTx, query, tuple)
      }
      else -> {
        Future.failedFuture("${event.javaClass.simpleName} does not have any event projector handler")
      }
    }
  }
}
