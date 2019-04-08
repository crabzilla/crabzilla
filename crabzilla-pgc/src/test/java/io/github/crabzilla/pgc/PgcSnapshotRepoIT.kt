package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.example1.*
import io.github.crabzilla.pgc.PgcUowJournal.Companion.SQL_INSERT_UOW
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.reactiverse.pgclient.Tuple
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Handler
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcSnapshotRepoIT {

  private lateinit var vertx: Vertx
  internal lateinit var writeDb: PgPool
  internal lateinit var repo: SnapshotRepository<Customer>

  companion object {
    const val aggregateName = "Customer"
    val customerId = CustomerId(1)
    val createCmd = CreateCustomer(UUID.randomUUID(), customerId, "customer")
    val created = CustomerCreated(customerId, "customer")
    val activateCmd = ActivateCustomer(UUID.randomUUID(), customerId, "I want it")
    val activated = CustomerActivated("a good reason", Instant.now())
  }

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    val vertxOptions = VertxOptions()

    vertx = Vertx.vertx(vertxOptions)

    initVertx(vertx)

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
        .setHost(config.getString("WRITE_DATABASE_HOST"))
        .setDatabase(config.getString("WRITE_DATABASE_NAME"))
        .setUser(config.getString("WRITE_DATABASE_USER"))
        .setPassword(config.getString("WRITE_DATABASE_PASSWORD"))
        .setMaxSize(config.getInteger("WRITE_DATABASE_POOL_MAX_SIZE"))

      writeDb = PgClient.pool(vertx, options)

      repo = PgcSnapshotRepo(writeDb, CUSTOMER_SEED_VALUE, CUSTOMER_STATE_BUILDER, Customer::class.java)

      writeDb.query("delete from units_of_work") { deleteResult ->
        if (deleteResult.failed()) {
          deleteResult.cause().printStackTrace()
          tc.failNow(deleteResult.cause())
          return@query
        }
        tc.completeNow()
      }

    })

  }


  @Test
  @DisplayName("given none snapshot or event, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext) {

      repo.retrieve(createCmd.targetId.id, aggregateName, Handler { event ->
        if (event.failed()) {
          event.cause().printStackTrace()
          tc.failNow(event.cause())
        }
        val snapshot: Snapshot<Customer> = event.result()
        assertThat(snapshot.version).isEqualTo(0)
        assertThat(snapshot.instance).isEqualTo(CUSTOMER_SEED_VALUE)
        tc.completeNow()
      })

  }

  @Test
  @DisplayName("given none snapshot and a created event, it can retrieve correct snapshot")
  fun a1(tc: VertxTestContext) {

    val tuple = Tuple.of(UUID.randomUUID(),
      io.reactiverse.pgclient.data.Json.create(listOfEventsToJson(listOf(created))),
      createCmd.commandId,
      io.reactiverse.pgclient.data.Json.create(commandToJson(createCmd)),
      aggregateName,
      customerId.value(),
      1)

    writeDb.preparedQuery(SQL_INSERT_UOW, tuple) { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
      }
      val uowSequence = ar1.result().first().getLong(0)
      assertThat(uowSequence).isGreaterThan(0)
      repo.retrieve(createCmd.targetId.id, aggregateName, Handler { event ->
          if (event.failed()) {
            event.cause().printStackTrace()
            tc.failNow(event.cause())
          }
          val snapshot: Snapshot<Customer> = event.result()
          assertThat(snapshot.version).isEqualTo(1)
          assertThat(snapshot.instance).isEqualTo(Customer(customerId, createCmd.name, false, null))
          tc.completeNow()
      })
    }

  }

  @Test
  @DisplayName("given none snapshot and both created and an activated events, it can retrieve correct snapshot")
  fun a2(tc: VertxTestContext) {

    val tuple1 = Tuple.of(UUID.randomUUID(),
      io.reactiverse.pgclient.data.Json.create(listOfEventsToJson(listOf(created))),
      createCmd.commandId,
      io.reactiverse.pgclient.data.Json.create(commandToJson(createCmd)),
      aggregateName,
      customerId.value(),
      1)

    writeDb.preparedQuery(SQL_INSERT_UOW, tuple1) { ar1 ->

      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
      }

      val tuple2 = Tuple.of(UUID.randomUUID(),
        io.reactiverse.pgclient.data.Json.create(listOfEventsToJson(listOf(activated))),
        activateCmd.commandId,
        io.reactiverse.pgclient.data.Json.create(commandToJson(activateCmd)),
        aggregateName,
        customerId.value(),
        2)

      writeDb.preparedQuery(SQL_INSERT_UOW, tuple2) { ar2 ->

        if (ar2.failed()) {
          ar2.cause().printStackTrace()
          tc.failNow(ar2.cause())
        }
        val uowSequence = ar1.result().first().getLong(0)
        assertThat(uowSequence).isGreaterThan(0)

        repo.retrieve(createCmd.targetId.id, aggregateName, Handler { event ->
          if (event.failed()) {
            event.cause().printStackTrace()
            tc.failNow(event.cause())
          }
          val snapshot: Snapshot<Customer> = event.result()
          assertThat(snapshot.version).isEqualTo(2)
          assertThat(snapshot.instance.customerId).isEqualTo(customerId)
          assertThat(snapshot.instance.name).isEqualTo(createCmd.name)
          assertThat(snapshot.instance.isActive).isEqualTo(true)
          tc.completeNow()
        })
      }
    }

  }

  // TODO given a snapshot and none events
  // TODO given a snapshot and some events, etc

}
