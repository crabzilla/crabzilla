package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.command.Snapshot
import io.github.crabzilla.core.command.SnapshotRepository
import io.github.crabzilla.pgc.example1.Customer
import io.github.crabzilla.pgc.example1.CustomerCommandAware
import io.github.crabzilla.pgc.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.createCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
import io.github.crabzilla.pgc.example1.Example1Fixture.example1Json
import io.github.crabzilla.pgc.writeModelPgPool
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
import kotlinx.serialization.encodeToString
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

  val SQL_APPEND_UOW =
  """ insert into crabz_events (event_payload, ar_name, ar_id, version, cmd_id) values
     ($1, $2, $3, $4, $5) returning event_id"""

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
      writeDb.query("delete from crabz_events").execute { deleteResult1 ->
        if (deleteResult1.failed()) {
          deleteResult1.cause().printStackTrace()
          tc.failNow(deleteResult1.cause())
          return@execute
        }
        writeDb.query("delete from crabz_customer_snapshots").execute { deleteResult2 ->
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
    val eventJson: String = example1Json.encodeToString(DOMAIN_EVENT_SERIALIZER, created1)
    val tuple = Tuple.of(JsonObject(eventJson), CUSTOMER_ENTITY, customerId1, 1, UUID.randomUUID())
    writeDb.preparedQuery(SQL_APPEND_UOW)
      .execute(tuple) { event1 ->
      if (event1.failed()) {
        event1.cause().printStackTrace()
        tc.failNow(event1.cause())
        return@execute
      }
      val eventId = event1.result().first().getLong(0)
      tc.verify { assertThat(eventId).isGreaterThan(0) }
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
    val createdJson: String = example1Json.encodeToString(DOMAIN_EVENT_SERIALIZER, created1)
    val activatedJson: String = example1Json.encodeToString(DOMAIN_EVENT_SERIALIZER, activated1)
    val tuple1 = Tuple.of(JsonObject(createdJson), CUSTOMER_ENTITY, customerId1, 1, UUID.randomUUID())
    writeDb.preparedQuery(SQL_APPEND_UOW).execute(tuple1) { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@execute
      }
      val tuple2 = Tuple.of(JsonObject(activatedJson), CUSTOMER_ENTITY, customerId1, 2, UUID.randomUUID())
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
