package io.github.crabzilla.pgc

import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.EVENT_SERIALIZER
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.SnapshotRepository
import io.github.crabzilla.pgc.PgcUowJournal.Companion.SQL_APPEND_UOW
import io.github.crabzilla.pgc.example1.Customer
import io.github.crabzilla.pgc.example1.CustomerCommandAware
import io.github.crabzilla.pgc.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.pgc.example1.Example1Fixture.activateCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.createCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
import io.github.crabzilla.pgc.example1.Example1Fixture.example1Json
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
import java.util.UUID
import kotlinx.serialization.builtins.list
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

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
      repo = PgcSnapshotRepo(writeDb, example1Json, CustomerCommandAware())
      writeDb.query("delete from units_of_work").execute { deleteResult1 ->
        if (deleteResult1.failed()) {
          deleteResult1.cause().printStackTrace()
          tc.failNow(deleteResult1.cause())
          return@execute
        }
        writeDb.query("delete from customer_snapshots").execute { deleteResult2 ->
          if (deleteResult2.failed()) {
            deleteResult2.cause().printStackTrace()
            tc.failNow(deleteResult2.cause())
            return@execute
          }
          tc.completeNow()
        }
      }
    })
  }

  @Test
  @DisplayName("given none snapshot or event, it can retrieve correct snapshot")
  fun a0(tc: VertxTestContext) {
      repo.retrieve(customerId1).onComplete { event ->
        if (event.failed()) {
          event.cause().printStackTrace()
          tc.failNow(event.cause())
          return@onComplete
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
    val eventsJsonArray: String = example1Json.stringify(EVENT_SERIALIZER.list, listOf(created1))
    val commandJsonObject: String = example1Json.stringify(COMMAND_SERIALIZER, createCmd1)
    val tuple = Tuple.of(eventsJsonArray, UUID.randomUUID(), commandJsonObject, CUSTOMER_ENTITY, customerId1, 1)
    writeDb.preparedQuery(SQL_APPEND_UOW)
      .execute(tuple) { event1 ->
      if (event1.failed()) {
        event1.cause().printStackTrace()
        tc.failNow(event1.cause())
        return@execute
      }
      val uowId = event1.result().first().getLong(0)
      tc.verify { assertThat(uowId).isGreaterThan(0) }
      repo.retrieve(customerId1).onComplete { event2 ->
        if (event2.failed()) {
          event2.cause().printStackTrace()
          tc.failNow(event2.cause())
          return@onComplete
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
    val eventsJsonArray: String = example1Json.stringify(EVENT_SERIALIZER.list, listOf(created1, activated1))
    val commandJsonObject: String = example1Json.stringify(COMMAND_SERIALIZER, createCmd1)
    val tuple1 = Tuple.of(eventsJsonArray, UUID.randomUUID(), commandJsonObject, CUSTOMER_ENTITY, customerId1, 1)
    writeDb.preparedQuery(SQL_APPEND_UOW).execute(tuple1) { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@execute
      }
      val eventsJsonArray2: String = example1Json.stringify(EVENT_SERIALIZER.list, listOf(activated1))
      val commandJsonObject2: String = example1Json.stringify(COMMAND_SERIALIZER, activateCmd1)
      val tuple2 = Tuple.of(eventsJsonArray2, UUID.randomUUID(), commandJsonObject2, CUSTOMER_ENTITY, customerId1, 2)
      writeDb.preparedQuery(SQL_APPEND_UOW).execute(tuple2) { ar2 ->
        if (ar2.failed()) {
          tc.failNow(ar2.cause())
          return@execute
        }
        val uowId = ar1.result().first().getLong(0)
        tc.verify { assertThat(uowId).isGreaterThan(0) }
        repo.retrieve(customerId1).onComplete { event ->
          if (event.failed()) {
            tc.failNow(event.cause())
            return@onComplete
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
