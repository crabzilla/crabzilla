package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.command.COMMAND_SERIALIZER
import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.command.UnitOfWorkJournal
import io.github.crabzilla.core.command.UnitOfWorkRepository
import io.github.crabzilla.pgc.command.PgcUowJournal.Companion.SQL_APPEND_UOW
import io.github.crabzilla.pgc.command.PgcUowJournal.FullPayloadPublisher
import io.github.crabzilla.pgc.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.pgc.example1.Example1Fixture.activateCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.activatedUow1
import io.github.crabzilla.pgc.example1.Example1Fixture.createCmd1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.createdUow1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
import io.github.crabzilla.pgc.example1.Example1Fixture.example1Json
import io.github.crabzilla.pgc.writeModelPgPool
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcUowRepoIT {

  private lateinit var vertx: Vertx
  private lateinit var writeDb: PgPool
  private lateinit var repo: UnitOfWorkRepository
  private lateinit var testRepo: PgcUowTestRepo
  private lateinit var journal: UnitOfWorkJournal

  val eventsJsonArray = example1Json.stringify(DOMAIN_EVENT_SERIALIZER.list, listOf(created1))
  val commandJsonObject = example1Json.stringify(COMMAND_SERIALIZER, createCmd1)
  val tuple1 = Tuple.of(JsonArray(eventsJsonArray), createdUow1.commandId, JsonObject(commandJsonObject),
    CUSTOMER_ENTITY, customerId1, 1)

  val eventsJsonArray2 = example1Json.stringify(DOMAIN_EVENT_SERIALIZER.list, listOf(activated1))
  val commandJsonObject2 = example1Json.stringify(COMMAND_SERIALIZER, activateCmd1)
  val tuple2 = Tuple.of(JsonArray(eventsJsonArray2), activatedUow1.commandId, JsonObject(commandJsonObject2),
    CUSTOMER_ENTITY, customerId1, 2)

  @BeforeEach
  fun setup(tc: VertxTestContext) {
    vertx = Vertx.vertx()
    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))
    val options = ConfigRetrieverOptions().addStore(envOptions)
    val retriever = ConfigRetriever.create(vertx, options)
    retriever.getConfig { configFuture ->
      if (configFuture.failed()) {
        tc.failNow(configFuture.cause())
        return@getConfig
      }
      val config = configFuture.result()
      writeDb = writeModelPgPool(vertx, config)
      repo = PgcUowRepo(writeDb, example1Json)
      testRepo = PgcUowTestRepo(writeDb, example1Json)
      journal = PgcUowJournal(writeDb, example1Json, FullPayloadPublisher(vertx))
      writeDb.query("delete from crabz_units_of_work")
        .execute { deleteResult1 ->
          if (deleteResult1.failed()) {
            tc.failNow(deleteResult1.cause())
            return@execute
          }
          writeDb.query("delete from crabz_customer_snapshots")
            .execute { deleteResult2 ->
              if (deleteResult2.failed()) {
                tc.failNow(deleteResult2.cause())
                return@execute
              }
              tc.completeNow()
            }
        }
    }
  }

  @Test
  fun `selectLastUowId works `(tc: VertxTestContext) {
    writeDb.preparedQuery(SQL_APPEND_UOW)
      .execute(tuple1) { event1 ->
        if (event1.failed()) {
          event1.cause().printStackTrace()
          tc.failNow(event1.cause())
          return@execute
        }
        val uowId = event1.result().first().getLong(0)
        tc.verify { tc.verify { assertThat(uowId).isGreaterThan(0) } }
        repo.selectLastUowId()
          .onFailure { err -> tc.failNow(err) }
          .onSuccess { result ->
            tc.verify {
              assertThat(result).isEqualTo(uowId)
              tc.completeNow()
            }
          }
      }
  }

  @Test
  fun `can queries an unit of work row by it's command id`(tc: VertxTestContext) {
    writeDb.preparedQuery(SQL_APPEND_UOW)
      .execute(tuple1) { event1 ->
      if (event1.failed()) {
        event1.cause().printStackTrace()
        tc.failNow(event1.cause())
        return@execute
      }
      val uowId = event1.result().first().getLong(0)
      tc.verify { tc.verify { assertThat(uowId).isGreaterThan(0) } }
      repo.getUowByCmdId(createdUow1.commandId)
        .onFailure { err -> tc.failNow(err) }
        .onSuccess { uowPair ->
        tc.verify {
          if (uowPair == null) {
            tc.failNow(NullPointerException())
            return@verify
          }
          assertThat(uowPair.first).isEqualTo(createdUow1)
          assertThat(uowPair.second).isEqualTo(uowId)
          tc.completeNow()
        }
      }
    }
  }

  @Test
  @DisplayName("can queries an unit of work row by it's uow id")
  fun a5(tc: VertxTestContext) {
    writeDb.preparedQuery(SQL_APPEND_UOW)
      .execute(tuple1) { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@execute
      }
      val uowId = event1.result().first().getLong("uow_id")
      tc.verify { assertThat(uowId).isGreaterThan(0) }
      repo.getUowByUowId(uowId)
        .onFailure { err -> tc.failNow(err) }
        .onSuccess { uow ->
          tc.verify { assertThat(createdUow1).isEqualTo(uow) }
          tc.completeNow()
      }
    }
  }

  @Test
  @DisplayName("can query units of work for a given entity ID")
  fun a33(tc: VertxTestContext) {
    writeDb.preparedQuery(SQL_APPEND_UOW)
      .execute(tuple1) { event1 ->
        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@execute
        }
        writeDb.preparedQuery(SQL_APPEND_UOW)
          .execute(tuple2) { event2 ->
            if (event2.failed()) {
              tc.failNow(event2.cause())
              return@execute
            }
            repo.selectByAggregateRootId(customerId1)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { uowList ->
                tc.verify { assertThat(uowList.size).isEqualTo(2) }
                val uow1 = uowList[0]
                val uow2 = uowList[1]
                tc.verify { assertThat(uow1).isEqualTo(createdUow1) }
                tc.verify { assertThat(uow2).isEqualTo(activatedUow1) }
                tc.completeNow()
            }
          }
      }
  }
}
