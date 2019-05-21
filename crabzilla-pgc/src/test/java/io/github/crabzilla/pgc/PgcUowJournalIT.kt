package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.pgc.example1.Example1Fixture.activated1
import io.github.crabzilla.pgc.example1.Example1Fixture.activatedUow1
import io.github.crabzilla.pgc.example1.Example1Fixture.created1
import io.github.crabzilla.pgc.example1.Example1Fixture.createdUow1
import io.github.crabzilla.pgc.example1.Example1Fixture.customerEntityName
import io.github.crabzilla.pgc.example1.Example1Fixture.customerJson
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
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
  internal lateinit var writeDb: PgPool
  internal lateinit var repo: UnitOfWorkRepository
  internal lateinit var journal: UnitOfWorkJournal

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    val vertxOptions = VertxOptions()

    vertx = Vertx.vertx(vertxOptions)

    Crabzilla.initVertx(vertx)

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

      journal.append(createdUow1, Handler { event1 ->

        if (event1.failed()) {
          tc.failNow(event1.cause())
          return@Handler
        }

        val uowSequence: Int = event1.result()

        tc.verify { assertThat(uowSequence).isGreaterThan(0) }

        val uowFuture = Future.future<UnitOfWork>()

        repo.getUowByUowId(createdUow1.unitOfWorkId, uowFuture)

        uowFuture.setHandler { ar2 ->
          if (ar2.failed()) {
            tc.failNow(ar2.cause())
            return@setHandler
          }

          val uow = ar2.result()

         tc.verify { assertThat(uow).isEqualTo(createdUow1) }

          val snapshotDataFuture = Future.future<SnapshotEvents>()

          repo.selectAfterVersion(createdUow1.entityId, 0, customerEntityName, snapshotDataFuture)

          snapshotDataFuture.setHandler { ar3 ->
            if (ar3.failed()) {
              tc.failNow(ar3.cause())
              return@setHandler
            }

            val (version, events) = ar3.result()

            tc.verify { assertThat(version).isEqualTo(createdUow1.version) }

            tc.verify { assertThat(events).isEqualTo(createdUow1.events) }

            tc.completeNow()

          }
        }

      })

  }

  @Test
  @DisplayName("cannot append version 1 twice")
  fun s2(tc: VertxTestContext) {

    val appendFuture1 = Future.future<Int>()

    // append uow1
    journal.append(createdUow1, appendFuture1.completer())

    appendFuture1.setHandler { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@setHandler
      }

      val uowSequence = ar1.result()

      tc.verify { assertThat(uowSequence).isGreaterThan(0) }

      val appendFuture2 = Future.future<Int>()

      // try to append uow1 again
      journal.append(createdUow1, appendFuture2)

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
  @DisplayName("can append version 1 and version 2")
  fun s3(tc: VertxTestContext) {

    val appendFuture1 = Future.future<Int>()

    // append uow1
    journal.append(createdUow1, appendFuture1)

    appendFuture1.setHandler { ar1 ->

      if (ar1.failed()) {
        tc.failNow(ar1.cause())
      } else {

        val uowSequence = ar1.result()
        tc.verify { assertThat(uowSequence).isGreaterThan(0) }
        val appendFuture2 = Future.future<Int>()

        // append uow2
        journal.append(activatedUow1, appendFuture2)

        appendFuture2.setHandler { ar2 ->

          if (ar2.failed()) {
            tc.failNow(ar2.cause())

          } else {
            val uowSequence = ar2.result()
            tc.verify { assertThat(uowSequence).isGreaterThan(2) }
            val snapshotDataFuture = Future.future<SnapshotEvents>()

            // get all versions for id
            repo.selectAfterVersion(activatedUow1.entityId, 0, customerEntityName, snapshotDataFuture)

            snapshotDataFuture.setHandler { ar4 ->

              if (ar4.failed()) {
                tc.failNow(ar4.cause())

              } else {
                val (version, events) = ar4.result()
                tc.verify { assertThat(version).isEqualTo(activatedUow1.version) }
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
