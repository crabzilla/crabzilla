package io.github.crabzilla.pgc

import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.internal.UnitOfWorkJournal
import io.github.crabzilla.internal.UnitOfWorkRepository
import io.github.crabzilla.pgc.example1.Example1Fixture
import io.github.crabzilla.pgc.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.activatedUow1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.createdUow1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerId1
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
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcUowJournalIT {

  private lateinit var vertx: Vertx
  private lateinit var writeDb: PgPool
  private lateinit var repo: UnitOfWorkRepository
  private lateinit var journal: UnitOfWorkJournal

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
      repo = PgcUowRepo(writeDb, example1Json)
      journal = PgcUowJournal(vertx, writeDb, example1Json)
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
          println("deleted both read and write model tables")
          tc.completeNow()
        }
      }
    })
  }

  @Test
  @DisplayName("can append version 1")
  fun s1(tc: VertxTestContext) {
      journal.append(createdUow1).onComplete { event1 ->
        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@onComplete
        }
        val uowId: Long = event1.result()
        tc.verify { assertThat(uowId).isGreaterThan(0) }
        repo.getUowByUowId(uowId).onComplete { event2 ->
          if (event2.failed()) {
            tc.failNow(event2.cause())
            return@onComplete
          }
          val uow = event2.result()
          tc.verify { assertThat(uow).isEqualTo(createdUow1) }
          repo.selectAfterVersion(createdUow1.entityId, 0, CUSTOMER_ENTITY).onComplete { event3 ->
            if (event3.failed()) {
              tc.failNow(event3.cause())
              return@onComplete
            }
            val (afterVersion, untilVersion, events) = event3.result()
            tc.verify { assertThat(afterVersion).isEqualTo(0) }
            tc.verify { assertThat(untilVersion).isEqualTo(createdUow1.version) }
            tc.verify { assertThat(events).isEqualTo(createdUow1.events) }
            tc.completeNow()
          }
        }
      }
  }

  @Test
  @DisplayName("cannot append version 1 twice")
  fun s2(tc: VertxTestContext) {
    journal.append(createdUow1).onComplete { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@onComplete
      }
      val uowId = ar1.result()
      tc.verify { assertThat(uowId).isGreaterThan(0) }
      journal.append(createdUow1).onComplete { ar2 ->
        if (ar2.failed()) {
          tc.verify { assertThat(ar2.cause().message).isEqualTo("expected version is 0 but current version is 1") }
          tc.completeNow()
          return@onComplete
        }
      }
    }
  }

  @Test
  @DisplayName("cannot append version 3 after version 1")
  fun s22(tc: VertxTestContext) {
    val createdUow3 =
      UnitOfWork(CUSTOMER_ENTITY, customerId1, UUID.randomUUID(), Example1Fixture.createCmd1, 3, listOf(created1))
    // append uow1
    journal.append(createdUow1).onComplete { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@onComplete
      }
      val uowId = ar1.result()
      tc.verify { assertThat(uowId).isGreaterThan(0) }
      journal.append(createdUow3).onComplete { ar2 ->
        if (ar2.failed()) {
          tc.verify { assertThat(ar2.cause().message).isEqualTo("expected version is 2 but current version is 1") }
          tc.completeNow()
          return@onComplete
        }
      }
    }
  }

  @Test
  @DisplayName("can append version 1 and version 2")
  fun s3(tc: VertxTestContext) {
    journal.append(createdUow1).onComplete { ar1 ->
      if (ar1.failed()) {
        tc.failNow(ar1.cause())
      } else {
        val uowId1 = ar1.result()
        tc.verify { assertThat(uowId1).isGreaterThan(0) }
        journal.append(activatedUow1).onComplete { ar2 ->
          if (ar2.failed()) {
            tc.failNow(ar2.cause())
          } else {
            val uowId = ar2.result()
            tc.verify { assertThat(uowId).isGreaterThan(2) }
            // get all versions for id
            repo.selectAfterVersion(activatedUow1.entityId, 0, CUSTOMER_ENTITY).onComplete { ar4 ->
              if (ar4.failed()) {
                tc.failNow(ar4.cause())
              } else {
                val (afterVersion, untilVersion, events) = ar4.result()
                tc.verify { assertThat(afterVersion).isEqualTo(0) }
                tc.verify { assertThat(untilVersion).isEqualTo(activatedUow1.version) }
                tc.verify { assertThat(events.size).isEqualTo(2) }
                tc.verify { assertThat(events[0]).isEqualTo(created1) }
                tc.verify { assertThat(events[1]).isEqualToIgnoringGivenFields(activated1, "_when") }
                tc.completeNow()
              }
            }
          }
        }
      }
    }
  }
}
