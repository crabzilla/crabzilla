package io.github.crabzilla.pgc

import io.github.crabzilla.framework.Snapshot
import io.github.crabzilla.example1.aggregate.Customer
import io.github.crabzilla.example1.aggregate.CustomerCommandAware
import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.internal.SnapshotRepository
import io.github.crabzilla.pgc.PgcUowJournal.Companion.SQL_APPEND_UOW
import io.github.crabzilla.pgc.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.pgc.example1.Example1Fixture.activateCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.createCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerJson
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
import io.vertx.sqlclient.Tuple
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcSnapshotRepoIT {

  private lateinit var vertx: Vertx
  private lateinit var writeDb: PgPool
  private lateinit var repo: SnapshotRepository<Customer>

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    val vertxOptions = VertxOptions()

    vertx = Vertx.vertx(vertxOptions)

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

      writeDb = writeModelPgPool(vertx, config)

      repo = PgcSnapshotRepo(writeDb, CUSTOMER_ENTITY, CustomerCommandAware(), CustomerJsonAware())

      writeDb.query("delete from units_of_work") { deleteResult1 ->
        if (deleteResult1.failed()) {
          deleteResult1.cause().printStackTrace()
          tc.failNow(deleteResult1.cause())
          return@query
        }
        writeDb.query("delete from customer_snapshots") { deleteResult2 ->
          if (deleteResult2.failed()) {
            deleteResult2.cause().printStackTrace()
            tc.failNow(deleteResult2.cause())
            return@query
          }
          tc.completeNow()
        }
      }


    })

  }


  @Test
  @DisplayName("given none snapshot or event, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext) {

      repo.retrieve(customerId1.value).future().setHandler { event ->
        if (event.failed()) {
          event.cause().printStackTrace()
          tc.failNow(event.cause())
        }
        val snapshot: Snapshot<Customer> = event.result()
        tc.verify { assertThat(snapshot.version).isEqualTo(0) }
        tc.verify { assertThat(snapshot.state).isEqualTo(Customer()) }
        tc.completeNow()
      }

  }

  @Test
  @DisplayName("given none snapshot and a created event, it can retrieve correct snapshot")
  fun a1(tc: VertxTestContext) {

    val eventsAsJson = customerJson.toJsonArray(arrayListOf(Pair("CustomerCreated", created1)))

    val tuple = Tuple.of(eventsAsJson, UUID.randomUUID(),
      "create",
      customerJson.cmdToJson(createCmd1),
      CUSTOMER_ENTITY,
      customerId1.value,
      1)

    writeDb.preparedQuery(SQL_APPEND_UOW, tuple) { event1 ->
      if (event1.failed()) {
        event1.cause().printStackTrace()
        tc.failNow(event1.cause())
      }
      val uowId = event1.result().first().getLong(0)
      tc.verify { assertThat(uowId).isGreaterThan(0) }

      repo.retrieve(customerId1.value).future().setHandler { event2 ->
        if (event2.failed()) {
          event2.cause().printStackTrace()
          tc.failNow(event2.cause())
        }
        val snapshot: Snapshot<Customer> = event2.result()
        tc.verify { assertThat(snapshot.version).isEqualTo(1) }
        tc.verify { assertThat(snapshot.state).isEqualTo(Customer(customerId1, createCmd1.name, false, null)) }
        tc.completeNow()
      }
    }

  }

  @Test
  @DisplayName("given none snapshot and both created and an activated events, it can retrieve correct snapshot")
  fun a2(tc: VertxTestContext) {

    val eventsAsJson = customerJson.toJsonArray(
      arrayListOf(Pair("CustomerCreated", created1), Pair("CustomerActivated", activated1)))

    val tuple1 = Tuple.of(
      eventsAsJson,
      UUID.randomUUID(),
      "create",
      customerJson.cmdToJson(createCmd1),
      CUSTOMER_ENTITY,
      customerId1.value,
      1)

    writeDb.preparedQuery(SQL_APPEND_UOW, tuple1) { ar1 ->

      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
      }

      val tuple2 = Tuple.of(
        customerJson.toJsonArray(arrayListOf(Pair("CustomerActivated", activated1))),
        UUID.randomUUID(),
        "activate",
        customerJson.cmdToJson(activateCmd1),
        CUSTOMER_ENTITY,
        customerId1.value,
        2)

      writeDb.preparedQuery(SQL_APPEND_UOW, tuple2) { ar2 ->

        if (ar2.failed()) {
          tc.failNow(ar2.cause())
        }
        val uowId = ar1.result().first().getLong(0)
        tc.verify { assertThat(uowId).isGreaterThan(0) }

        repo.retrieve(customerId1.value).future().setHandler { event ->
          if (event.failed()) {
            tc.failNow(event.cause())
          }
          val snapshot: Snapshot<Customer> = event.result()
          tc.verify { assertThat(snapshot.version).isEqualTo(2) }
          tc.verify { assertThat(snapshot.state.customerId).isEqualTo(customerId1) }
          tc.verify { assertThat(snapshot.state.name).isEqualTo(createCmd1.name) }
          tc.verify { assertThat(snapshot.state.isActive).isEqualTo(true) }
          tc.completeNow()
        }
      }
    }

  }

  // TODO given a snapshot and none events
  // TODO given a snapshot and some events, etc

}
