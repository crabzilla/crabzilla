package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.command.COMMAND_SERIALIZER
import io.github.crabzilla.core.command.EVENT_SERIALIZER
import io.github.crabzilla.core.command.UnitOfWorkJournal
import io.github.crabzilla.core.command.UnitOfWorkRepository
import io.github.crabzilla.pgc.command.PgcUowJournal.Companion.SQL_APPEND_UOW
import io.github.crabzilla.pgc.command.PgcUowTestRepo.RangeOfEvents
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcUowTestRepoIT {

  private lateinit var vertx: Vertx
  private lateinit var writeDb: PgPool
  private lateinit var repo: UnitOfWorkRepository
  private lateinit var testRepo: PgcUowTestRepo
  private lateinit var journal: UnitOfWorkJournal

  val eventsJsonArray = example1Json.stringify(EVENT_SERIALIZER.list, listOf(created1))
  val commandJsonObject = example1Json.stringify(COMMAND_SERIALIZER, createCmd1)
  val tuple1 = Tuple.of(JsonArray(eventsJsonArray), createdUow1.commandId, JsonObject(commandJsonObject),
    CUSTOMER_ENTITY, customerId1, 1)

  val eventsJsonArray2 = example1Json.stringify(EVENT_SERIALIZER.list, listOf(activated1))
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
      journal = PgcUowJournal(vertx, writeDb, example1Json)
      writeDb.query("delete from units_of_work")
        .execute { deleteResult1 ->
          if (deleteResult1.failed()) {
            tc.failNow(deleteResult1.cause())
            return@execute
          }
          writeDb.query("delete from customer_snapshots")
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
      repo.getUowByCmdId(createdUow1.commandId).onComplete { event2 ->
        if (event2.failed()) {
          tc.failNow(event2.cause())
          return@onComplete
        }
        val uowPair = event2.result()
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
      repo.getUowByUowId(uowId).onComplete { event2 ->
        if (event2.failed()) {
          tc.failNow(event2.cause())
          return@onComplete
        }
        val uow = event2.result()
        tc.verify { assertThat(createdUow1).isEqualTo(uow) }
        tc.completeNow()
      }
    }
  }

  @Nested
  @DisplayName("When selecting by uow sequence")
  @ExtendWith(VertxExtension::class)
  inner class WhenSelectingByUowSeq {

    @Test
    @DisplayName("can queries an empty repo")
    fun a1(tc: VertxTestContext) {
      testRepo.selectAfterUowId(1, 100, CUSTOMER_ENTITY).onComplete { event1 ->
        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@onComplete
        }
        val snapshotData = event1.result()
        tc.verify { assertThat(snapshotData.size).isEqualTo(0) }
        tc.completeNow()
      }
    }

    @Test
    @DisplayName("can queries a single unit of work row")
    fun a2(tc: VertxTestContext) {
      writeDb.preparedQuery(SQL_APPEND_UOW)
        .execute(tuple1) { event1 ->
        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@execute
        }
        val uowId = event1.result().first().getLong(0)
        tc.verify { assertThat(uowId).isGreaterThan(0) }
        val selectFuture = testRepo.selectAfterUowId(0, 100, CUSTOMER_ENTITY)
        selectFuture.onComplete { event2 ->
          if (event2.failed()) {
            tc.failNow(event2.cause())
            return@onComplete
          }
          val snapshotData = event2.result()
          tc.verify { assertThat(snapshotData.size).isEqualTo(1) }
          val (uowId, targetId, events) = snapshotData[0]
          tc.verify { assertThat(uowId).isGreaterThan(0) }
          tc.verify { assertThat(targetId).isEqualTo(customerId1) }
          tc.verify { assertThat(events).isEqualTo(arrayListOf(created1)) }
          tc.completeNow()
        }
      }
    }

    @Test
    @DisplayName("can queries two unit of work rows")
    fun a3(tc: VertxTestContext) {
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
            testRepo.selectAfterUowId(0, 100, CUSTOMER_ENTITY).onComplete { event3 ->
            if (event3.failed()) {
              tc.failNow(event3.cause())
              return@onComplete
            }
            val snapshotData = event3.result()
            tc.verify { assertThat(snapshotData.size).isEqualTo(2) }
            val (uowId1, targetId1, events1) = snapshotData[0]
            tc.verify { assertThat(uowId1).isGreaterThan(0) }
            tc.verify { assertThat(targetId1).isEqualTo(customerId1) }
            tc.verify { assertThat(events1).isEqualTo(listOf(created1)) }
            val (uowId2, targetId2, events2) = snapshotData[1]
            tc.verify { assertThat(uowId2).isEqualTo(uowId1.inc()) }
            tc.verify { assertThat(targetId2).isEqualTo(customerId1) }
            tc.verify { assertThat(events2).isEqualTo(listOf(activated1)) }
            tc.completeNow()
          }
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
          repo.selectByEntityId(customerId1).onComplete { event3 ->
            if (event3.failed()) {
              tc.failNow(event2.cause())
              return@onComplete
            }
            val uowList = event3.result()
            println(uowList)
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

    @Test
    @DisplayName("can queries by uow sequence")
    fun a4(tc: VertxTestContext) {
      writeDb.preparedQuery(SQL_APPEND_UOW)
        .execute(tuple1) { event1 ->
        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@execute
        }
        val uowId1 = event1.result().first().getLong("uow_id")
        writeDb.preparedQuery(SQL_APPEND_UOW)
          .execute(tuple2) { event2 ->
          if (event2.failed()) {
            tc.failNow(event2.cause())
            return@execute
          }
          val uowId2 = event2.result().first().getLong("uow_id")
            testRepo.selectAfterUowId(uowId1, 100, CUSTOMER_ENTITY).onComplete { event3 ->
            if (event3.failed()) {
              tc.failNow(event3.cause())
              return@onComplete
            }
            val snapshotData = event3.result()
            tc.verify { assertThat(snapshotData.size).isEqualTo(1) }
            val (uowId, targetId, events) = snapshotData[0]
            tc.verify { assertThat(uowId).isEqualTo(uowId2) }
            tc.verify { assertThat(targetId).isEqualTo(customerId1) }
            tc.verify { assertThat(events).isEqualTo(arrayListOf(activated1)) }
            tc.completeNow()
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("When selecting by version")
  @ExtendWith(VertxExtension::class)
  inner class WhenSelectingByVersion {

    @Test
    @DisplayName("can queries a single unit of work row")
    fun a2(tc: VertxTestContext) {
      writeDb.preparedQuery(SQL_APPEND_UOW)
        .execute(tuple1) { event1 ->
        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@execute
        }
        val uowId = event1.result().first().getLong(0)
        tc.verify { assertThat(uowId).isGreaterThan(0) }
          testRepo.selectAfterVersion(customerId1, 0, CUSTOMER_ENTITY).onComplete { event2 ->
          if (event2.failed()) {
            tc.failNow(event2.cause())
            return@onComplete
          }
          val snapshotData = event2.result()
          tc.verify { assertThat(1).isEqualTo(snapshotData.untilVersion) }
          tc.verify { assertThat(arrayListOf(created1)).isEqualTo(snapshotData.events) }
          tc.completeNow()
        }
      }
    }

    @Test
    @DisplayName("can queries two unit of work rows")
    fun a3(tc: VertxTestContext) {
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
            testRepo.selectAfterVersion(customerId1, 0, CUSTOMER_ENTITY).onComplete { event3 ->
            if (event3.failed()) {
              tc.failNow(event3.cause())
              return@onComplete
            }
            val snapshotData = event3.result()
            tc.verify { assertThat(2).isEqualTo(snapshotData.untilVersion) }
            tc.verify { assertThat(listOf(created1, activated1)).isEqualTo(snapshotData.events) }
            tc.completeNow()
          }
        }
      }
    }

    @Test
    @DisplayName("can queries by version")
    fun a4(tc: VertxTestContext) {
      writeDb.preparedQuery(SQL_APPEND_UOW)
        .execute(tuple1) { event1 ->
        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@execute
        }
        val uowId1 = event1.result().first().getLong("uow_id")
        writeDb.preparedQuery(SQL_APPEND_UOW)
          .execute(tuple2) { event2 ->
          if (event2.failed()) {
            tc.failNow(event2.cause())
            return@execute
          }
          val uowId2 = event2.result().first().getLong("uow_id")
            testRepo.selectAfterVersion(customerId1, 1, CUSTOMER_ENTITY).onComplete { event3 ->
            if (event3.failed()) {
              tc.failNow(event3.cause())
              return@onComplete
            }
            val snapshotData: RangeOfEvents = event3.result()
            tc.verify { assertThat(2).isEqualTo(snapshotData.untilVersion) }
            tc.verify { assertThat(listOf(activated1)).isEqualTo(snapshotData.events) }
            tc.completeNow()
          }
        }
      }
    }
  }

  @Test
  @DisplayName("can queries only above version 1")
  fun s4(tc: VertxTestContext) {
    journal.append(createdUow1).onComplete { event1 ->
      if (event1.failed()) {
        tc.failNow(event1.cause())
        return@onComplete
      }
      val uowId = event1.result()
      tc.verify { assertThat(uowId).isGreaterThan(0) }
      // append uow2
      journal.append(activatedUow1).onComplete { event2 ->
        if (event2.failed()) {
          tc.failNow(event2.cause())
          return@onComplete
        }
        val uowId = event2.result()
        tc.verify { assertThat(uowId).isGreaterThan(2) }
        // get only above version 1
        testRepo.selectAfterVersion(activatedUow1.entityId, 1, CUSTOMER_ENTITY).onComplete { event3 ->
          if (event3.failed()) {
            tc.failNow(event3.cause())
            return@onComplete
          }
          val (afterVersion, untilVersion, events) = event3.result()
          tc.verify { assertThat(afterVersion).isEqualTo(1) }
          tc.verify { assertThat(untilVersion).isEqualTo(activatedUow1.version) }
          tc.verify { assertThat(events).isEqualTo(arrayListOf(activated1)) }
          tc.completeNow()
        }
      }
    }
  }
}
