package io.github.crabzilla.pgc

import io.github.crabzilla.example1.aggregate.Customer
import io.github.crabzilla.framework.UnitOfWork
import io.github.crabzilla.internal.UnitOfWorkJournal
import io.github.crabzilla.internal.UnitOfWorkRepository
import io.github.crabzilla.pgc.example1.Example1Fixture
import io.github.crabzilla.pgc.example1.Example1Fixture.CUSTOMER_ENTITY
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.activatedUow1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.createdUow1
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcUowJournalIT {

  private lateinit var vertx: Vertx
  private lateinit var writeDb: PgPool
  private lateinit var repo: UnitOfWorkRepository
  private lateinit var journal: UnitOfWorkJournal<Customer>

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
      repo = PgcUowRepo(writeDb, customerJson)
      journal = PgcUowJournal(writeDb, customerJson)

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
          println("deleted both read and write model tables")
          tc.completeNow()
        }
      }
    })

  }

  @Test
  @DisplayName("can append version 1")
  fun s1(tc: VertxTestContext) {

      journal.append(createdUow1).future().setHandler(Handler { event1 ->

        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@Handler
        }

        val uowId: Long = event1.result()

        tc.verify { assertThat(uowId).isGreaterThan(0) }

        val uowFuture = repo.getUowByUowId(uowId).future()

        uowFuture.setHandler { ar2 ->

          if (ar2.failed()) {
            tc.failNow(ar2.cause())
            return@setHandler
          }

          val uow = ar2.result()

          tc.verify { assertThat(uow).isEqualTo(createdUow1) }

          val rangeOfEventsFuture = repo.selectAfterVersion(createdUow1.entityId, 0, CUSTOMER_ENTITY).future()

          rangeOfEventsFuture.setHandler { ar3 ->
            if (ar3.failed()) {
              tc.failNow(ar3.cause())
              return@setHandler
            }

            val (afterVersion, untilVersion, events) = ar3.result()

            tc.verify { assertThat(afterVersion).isEqualTo(0) }

            tc.verify { assertThat(untilVersion).isEqualTo(createdUow1.version) }

            tc.verify { assertThat(events).isEqualTo(createdUow1.events) }

            tc.completeNow()

          }
        }

      })

  }

  @Test
  @DisplayName("cannot append version 1 twice")
  fun s2(tc: VertxTestContext) {

    val appendFuture1 = journal.append(createdUow1).future()

    appendFuture1.setHandler { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@setHandler
      }

      val uowId = ar1.result()

      tc.verify { assertThat(uowId).isGreaterThan(0) }

      val appendFuture2 = journal.append(createdUow1).future()

      appendFuture2.setHandler { ar2 ->
        if (ar2.failed()) {
          tc.verify { assertThat(ar2.cause().message).isEqualTo("expected version is 0 but current version is 1") }
          tc.completeNow()
          return@setHandler
        }

      }

    }

  }


  @Test
  @DisplayName("cannot append version 3 after version 1")
  fun s22(tc: VertxTestContext) {

    val createdUow3 = UnitOfWork(CUSTOMER_ENTITY, customerId1.value, UUID.randomUUID(),
      "create", Example1Fixture.createCmd1, 3, listOf(Pair("CustomerCreated", created1)))

    // append uow1
    val appendFuture1 = journal.append(createdUow1).future()

    appendFuture1.setHandler { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@setHandler
      }

      val uowId = ar1.result()

      tc.verify { assertThat(uowId).isGreaterThan(0) }

      val appendFuture2 = journal.append(createdUow3).future()

      appendFuture2.setHandler { ar2 ->
        if (ar2.failed()) {
          tc.verify { assertThat(ar2.cause().message).isEqualTo("expected version is 2 but current version is 1") }
          tc.completeNow()
          return@setHandler
        }

      }

    }

  }

  @Test
  @DisplayName("can append version 1 and version 2")
  fun s3(tc: VertxTestContext) {

    val appendFuture1 = journal.append(createdUow1).future()

    appendFuture1.setHandler { ar1 ->

      if (ar1.failed()) {
        tc.failNow(ar1.cause())
      } else {

        val uowId1 = ar1.result()
        tc.verify { assertThat(uowId1).isGreaterThan(0) }

        val appendFuture2 = journal.append(activatedUow1).future()

        appendFuture2.setHandler { ar2 ->

          if (ar2.failed()) {
            tc.failNow(ar2.cause())

          } else {
            val uowId = ar2.result()
            tc.verify { assertThat(uowId).isGreaterThan(2) }

            // get all versions for id
            val rangeOfEventsFuture = repo.selectAfterVersion(activatedUow1.entityId, 0, CUSTOMER_ENTITY).future()

            rangeOfEventsFuture.setHandler { ar4 ->

              if (ar4.failed()) {
                tc.failNow(ar4.cause())

              } else {
                val (afterVersion, untilVersion, events) = ar4.result()
                tc.verify { assertThat(afterVersion).isEqualTo(0) }
                tc.verify { assertThat(untilVersion).isEqualTo(activatedUow1.version) }
                tc.verify { assertThat(events.size).isEqualTo(2) }
                tc.verify { assertThat(events[0]).isEqualTo(Pair("CustomerCreated", created1)) }
                tc.verify { assertThat(events[1]).isEqualToIgnoringGivenFields(
                  Pair("CustomerActivated", activated1), "_when") }
                tc.completeNow()
              }

            }
          }
        }
      }
    }
  }
}
