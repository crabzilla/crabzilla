package io.github.crabzilla.pgc

import io.github.crabzilla.*
import io.github.crabzilla.example1.*
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
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgcUowJournalIT {

  private lateinit var vertx: Vertx
  internal lateinit var writeDb: PgPool
  internal lateinit var repo: UnitOfWorkRepository
  internal lateinit var journal: UnitOfWorkJournal

  companion object {
    const val aggregateName = "Customer"
    val customerId = CustomerId(1)
    val createCmd = CreateCustomer(UUID.randomUUID(), customerId, "customer")
    val created = CustomerCreated(customerId, "customer")
    val expectedUow1 = UnitOfWork(UUID.randomUUID(), createCmd, 1, listOf(created))
    val activateCmd = ActivateCustomer(UUID.randomUUID(), customerId, "I want it")
    val activated = CustomerActivated("a good reason", Instant.now())
    val expectedUow2 = UnitOfWork(UUID.randomUUID(), activateCmd, 2, listOf(activated))
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

      repo = PgcUowRepo(writeDb)
      journal = PgcUowJournal(writeDb)

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

  @Nested
  @DisplayName("When appending")
  @ExtendWith(VertxExtension::class)
  inner class WhenAppending {

    @Test
    @DisplayName("can append version 1")
    fun s1(tc: VertxTestContext) {

      val appendFuture = Future.future<Int>()

      journal.append(expectedUow1, aggregateName, appendFuture)

      appendFuture.setHandler { ar1 ->
        if (ar1.failed()) {
          ar1.cause().printStackTrace()
          tc.failNow(ar1.cause())
          return@setHandler
        }

        val uowSequence = ar1.result()

        assertThat(uowSequence).isGreaterThan(0)

        val uowFuture = Future.future<UnitOfWork>()

        repo.getUowByUowId(expectedUow1.unitOfWorkId, uowFuture)

        uowFuture.setHandler { ar2 ->
          if (ar2.failed()) {
            ar2.cause().printStackTrace()
            tc.failNow(ar2.cause())
            return@setHandler
          }

          val uow = ar2.result()

          assertThat(uow).isEqualTo(expectedUow1)

          val snapshotDataFuture = Future.future<SnapshotData>()

          repo.selectAfterVersion(expectedUow1.targetId().value(), 0, aggregateName, snapshotDataFuture.completer())

          snapshotDataFuture.setHandler { ar3 ->
            if (ar3.failed()) {
              ar3.cause().printStackTrace()
              tc.failNow(ar3.cause())
              return@setHandler
            }

            val (version, events) = ar3.result()

            assertThat(version).isEqualTo(expectedUow1.version)
            assertThat(events).isEqualTo(expectedUow1.events)

            tc.completeNow()

          }
        }
      }

    }

    @Test
    @DisplayName("cannot append version 1 twice")
    fun s2(tc: VertxTestContext) {

      val appendFuture1 = Future.future<Int>()

      // append uow1
      journal.append(expectedUow1, aggregateName, appendFuture1.completer())

      appendFuture1.setHandler { ar1 ->
        if (ar1.failed()) {
          ar1.cause().printStackTrace()
          tc.failNow(ar1.cause())
          return@setHandler
        }

        val uowSequence = ar1.result()

        assertThat(uowSequence).isGreaterThan(0)

        val appendFuture2 = Future.future<Int>()

        // try to append uow1 again
        journal.append(expectedUow1, aggregateName, appendFuture2.completer())

        appendFuture2.setHandler { ar2 ->
          if (ar2.failed()) {
            assertThat(ar2.cause()).isInstanceOf(DbConcurrencyException::class.java)
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
      journal.append(expectedUow1, aggregateName, appendFuture1)

      appendFuture1.setHandler { ar1 ->

        if (ar1.failed()) {
          ar1.cause().printStackTrace()
          tc.failNow(ar1.cause())
        } else {

          val uowSequence = ar1.result()
          assertThat(uowSequence).isGreaterThan(0)
          val appendFuture2 = Future.future<Int>()

          // append uow2
          journal.append(expectedUow2, aggregateName, appendFuture2)

          appendFuture2.setHandler { ar2 ->

            if (ar2.failed()) {
              ar2.cause().printStackTrace()
              tc.failNow(ar2.cause())

            } else {
              val uowSequence = ar2.result()
              assertThat(uowSequence).isGreaterThan(2)
              val snapshotDataFuture = Future.future<SnapshotData>()

              // get all versions for id
              repo.selectAfterVersion(expectedUow2.targetId().value(), 0, aggregateName, snapshotDataFuture.completer())

              snapshotDataFuture.setHandler { ar4 ->

                if (ar4.failed()) {
                  ar4.cause().printStackTrace()
                  tc.failNow(ar4.cause())

                } else {
                  val (version, events) = ar4.result()
                  assertThat(version).isEqualTo(expectedUow2.version)
                  assertThat(events).isEqualTo(listOf(created, activated))
                  tc.completeNow()
                }

              }
            }
          }
        }
      }
    }
  }

}
