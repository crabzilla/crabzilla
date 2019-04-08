package io.github.crabzilla.pgclient

import io.github.crabzilla.*
import io.github.crabzilla.example1.*
import io.github.crabzilla.pgclient.PgClientUowRepo.Companion.SQL_INSERT_UOW
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.reactiverse.pgclient.Tuple
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
class PgClientUowRepoIT {

  private lateinit var vertx: Vertx
  internal lateinit var writeDb: PgPool
  internal lateinit var repo: UnitOfWorkRepository

  companion object {
    val aggregateName = CommandHandlers.CUSTOMER.name
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
    vertxOptions.blockedThreadCheckInterval = (1000 * 60 * 60).toLong() // to easier debug

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

      repo = PgClientUowRepo(writeDb)

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
  @DisplayName("can queries an unit of work row by it's command id")
  fun a4(tc: VertxTestContext) {

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
      val selectFuture = Future.future<UnitOfWork>()
      selectFuture.setHandler { ar2 ->
        if (ar2.failed()) {
          ar2.cause().printStackTrace()
          tc.failNow(ar2.cause())
        }
        val uow = ar2.result()
        assertThat(expectedUow1).isEqualToIgnoringGivenFields(uow, "unitOfWorkId")
        tc.completeNow()
      }
      repo.getUowByCmdId(createCmd.commandId, selectFuture)
    }

  }

  @Test
  @DisplayName("can queries an unit of work row by it's uow id")
  fun a5(tc: VertxTestContext) {

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
      val selectFuture = Future.future<UnitOfWork>()
      selectFuture.setHandler { ar2 ->
        if (ar2.failed()) {
          ar2.cause().printStackTrace()
          tc.failNow(ar2.cause())
        }
        val uow = ar2.result()
        assertThat(expectedUow1).isEqualToIgnoringGivenFields(uow, "unitOfWorkId")
        tc.completeNow()
      }
      repo.getUowByUowId(tuple.getUUID(0), selectFuture)
    }

  }

  @Nested
  @DisplayName("When selecting by uow sequence")
  @ExtendWith(VertxExtension::class)
  inner class WhenSelectingByUowSeq {

    @Test
    @DisplayName("can queries an empty repo")
    fun a1(tc: VertxTestContext) {
      val selectFuture = Future.future<List<ProjectionData>>()
      repo.selectAfterUowSequence(0, 100, selectFuture)
      selectFuture.setHandler { selectAsyncResult ->
        val snapshotData = selectAsyncResult.result()
        assertThat(snapshotData.size).isEqualTo(0)
        tc.completeNow()
      }
    }

    @Test
    @DisplayName("can queries a single unit of work row")
    fun a2(tc: VertxTestContext) {

      val tuple = Tuple.of(UUID.randomUUID(),
        io.reactiverse.pgclient.data.Json.create(listOfEventsToJson(listOf(created))),
        createCmd.commandId,
        io.reactiverse.pgclient.data.Json.create(commandToJson(createCmd)),
        aggregateName,
        customerId.value(),
        1)

      val selectFuture = Future.future<List<ProjectionData>>()

      writeDb.preparedQuery(SQL_INSERT_UOW, tuple) { ar ->
        if (ar.failed()) {
          ar.cause().printStackTrace()
          tc.failNow(ar.cause())
        }
        val uowSequence = ar.result().first().getLong(0)
        assertThat(uowSequence).isGreaterThan(0)
        repo.selectAfterUowSequence(0, 100, selectFuture)
        selectFuture.setHandler { selectAsyncResult ->
          val snapshotData = selectAsyncResult.result()
          assertThat(snapshotData.size).isEqualTo(1)
          val (_, uowSequence, targetId, events) = snapshotData[0]
          assertThat(uowSequence).isGreaterThan(0)
          assertThat(targetId).isEqualTo(customerId.value())
          assertThat(events).isEqualTo(listOf(created))
          tc.completeNow()
        }
      }
    }

    @Test
    @DisplayName("can queries two unit of work rows")
    fun a3(tc: VertxTestContext) {

      val tuple1 = Tuple.of(UUID.randomUUID(),
        io.reactiverse.pgclient.data.Json.create(listOfEventsToJson(listOf(created))),
        createCmd.commandId,
        io.reactiverse.pgclient.data.Json.create(commandToJson(createCmd)),
        aggregateName,
        customerId.value(),
        1)

      val selectFuture1 = Future.future<List<ProjectionData>>()

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
          repo.selectAfterUowSequence(0, 100, selectFuture1)
          selectFuture1.setHandler { selectAsyncResult ->
            val snapshotData = selectAsyncResult.result()
            assertThat(snapshotData.size).isEqualTo(2)
            val (_, uowSequence1, targetId1, events1) = snapshotData[0]
            assertThat(uowSequence1).isGreaterThan(0)
            assertThat(targetId1).isEqualTo(customerId.value())
            assertThat(events1).isEqualTo(listOf(created))
            val (_, uowSequence2, targetId2, events2) = snapshotData[1]
            assertThat(uowSequence2).isEqualTo(uowSequence1 + 1)
            assertThat(targetId2).isEqualTo(customerId.value())
            assertThat(events2).isEqualTo(listOf(activated))
            tc.completeNow()
          }
        }

      }
    }

    @Test
    @DisplayName("can queries by uow sequence")
    fun a4(tc: VertxTestContext) {

      val tuple1 = Tuple.of(UUID.randomUUID(),
        io.reactiverse.pgclient.data.Json.create(listOfEventsToJson(listOf(created))),
        createCmd.commandId,
        io.reactiverse.pgclient.data.Json.create(commandToJson(createCmd)),
        aggregateName,
        customerId.value(),
        1)

      val selectFuture1 = Future.future<List<ProjectionData>>()

      writeDb.preparedQuery(SQL_INSERT_UOW, tuple1) { ar1 ->
        if (ar1.failed()) {
          ar1.cause().printStackTrace()
          tc.failNow(ar1.cause())
        }
        val uowSequence1 = ar1.result().first().getInteger("uow_seq_number")
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
          val uowSequence2 = ar2.result().first().getInteger("uow_seq_number")
          repo.selectAfterUowSequence(uowSequence1, 100, selectFuture1)
          selectFuture1.setHandler { selectAsyncResult ->
            val snapshotData = selectAsyncResult.result()
            assertThat(snapshotData.size).isEqualTo(1)
            val (_, uowSequence, targetId, events) = snapshotData[0]
            assertThat(uowSequence).isEqualTo(uowSequence2)
            assertThat(targetId).isEqualTo(customerId.value())
            assertThat(events).isEqualTo(listOf(activated))
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

      val tuple = Tuple.of(UUID.randomUUID(),
        io.reactiverse.pgclient.data.Json.create(listOfEventsToJson(listOf(created))),
        createCmd.commandId,
        io.reactiverse.pgclient.data.Json.create(commandToJson(createCmd)),
        aggregateName,
        customerId.value(),
        1)

      writeDb.preparedQuery(SQL_INSERT_UOW, tuple) { ar ->
        if (ar.failed()) {
          ar.cause().printStackTrace()
          tc.failNow(ar.cause())
        }
        val uowSequence = ar.result().first().getLong(0)
        assertThat(uowSequence).isGreaterThan(0)
        val selectFuture = Future.future<SnapshotData>()
        repo.selectAfterVersion(customerId.value(), 0, aggregateName, selectFuture.completer())
        selectFuture.setHandler { selectAsyncResult ->
          val snapshotData = selectAsyncResult.result()
          assertThat(1).isEqualTo(snapshotData.version)
          assertThat(listOf(created)).isEqualTo(snapshotData.events)
          tc.completeNow()
        }
      }

    }

    @Test
    @DisplayName("can queries two unit of work rows")
    fun a3(tc: VertxTestContext) {

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
          val selectFuture1 = Future.future<SnapshotData>()
          repo.selectAfterVersion(customerId.value(), 0, aggregateName, selectFuture1.completer())
          selectFuture1.setHandler { selectAsyncResult ->
            val snapshotData = selectAsyncResult.result()
            assertThat(2).isEqualTo(snapshotData.version)
            assertThat(listOf(created, activated)).isEqualTo(snapshotData.events)
            tc.completeNow()
          }
        }
      }

    }

    @Test
    @DisplayName("can queries by version")
    fun a4(tc: VertxTestContext) {

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
          return@preparedQuery
        }
        val uowSequence1 = ar1.result().first().getLong("uow_seq_number")
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
            return@preparedQuery
          }
          val uowSequence2 = ar2.result().first().getLong("uow_seq_number")
          val selectFuture1 = Future.future<SnapshotData>()
          repo.selectAfterVersion(customerId.value(), 1, aggregateName, selectFuture1.completer())
          selectFuture1.setHandler { selectAsyncResult ->
            val snapshotData = selectAsyncResult.result()
            assertThat(2).isEqualTo(snapshotData.version)
            assertThat(listOf(activated)).isEqualTo(snapshotData.events)
            tc.completeNow()
          }
        }
      }
    }
  }

  @Test
  @DisplayName("can queries only above version 1")
  fun s4(tc: VertxTestContext) {

    val appendFuture1 = Future.future<Int>()

    // append uow1
    repo.append(expectedUow1, aggregateName, appendFuture1)

    appendFuture1.setHandler { ar1 ->
      if (ar1.failed()) {
        ar1.cause().printStackTrace()
        tc.failNow(ar1.cause())
        return@setHandler
      }
      val uowSequence = ar1.result()
      assertThat(uowSequence).isGreaterThan(0)
      val appendFuture2 = Future.future<Int>()
      // append uow2
      repo.append(expectedUow2, aggregateName, appendFuture2)
      appendFuture2.setHandler { ar2 ->
        if (ar2.failed()) {
          ar2.cause().printStackTrace()
          tc.failNow(ar2.cause())
          return@setHandler
        }
        val uowSequence = ar2.result()
        assertThat(uowSequence).isGreaterThan(2)
        val snapshotDataFuture = Future.future<SnapshotData>()
        // get only above version 1
        repo.selectAfterVersion(expectedUow2.targetId().value(), 1, aggregateName, snapshotDataFuture.completer())
        snapshotDataFuture.setHandler { ar4 ->
          if (ar4.failed()) {
            ar4.cause().printStackTrace()
            tc.failNow(ar4.cause())
            return@setHandler
          }
          val (version, events) = ar4.result()
          assertThat(version).isEqualTo(expectedUow2.version)
          assertThat(events).isEqualTo(listOf(activated))
          tc.completeNow()
        }
      }
    }

  }

  @Nested
  @DisplayName("When appending")
  @ExtendWith(VertxExtension::class)
  inner class WhenAppending {

    @Test
    @DisplayName("can append version 1")
    fun s1(tc: VertxTestContext) {

      val appendFuture = Future.future<Int>()

      repo.append(expectedUow1, aggregateName, appendFuture)

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
      repo.append(expectedUow1, aggregateName, appendFuture1.completer())

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
        repo.append(expectedUow1, aggregateName, appendFuture2.completer())

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
      repo.append(expectedUow1, aggregateName, appendFuture1)

      appendFuture1.setHandler { ar1 ->

        if (ar1.failed()) {
          ar1.cause().printStackTrace()
          tc.failNow(ar1.cause())
        } else {

          val uowSequence = ar1.result()
          assertThat(uowSequence).isGreaterThan(0)
          val appendFuture2 = Future.future<Int>()

          // append uow2
          repo.append(expectedUow2, aggregateName, appendFuture2)

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
