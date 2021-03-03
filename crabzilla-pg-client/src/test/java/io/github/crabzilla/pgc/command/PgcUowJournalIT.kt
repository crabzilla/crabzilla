package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.UnitOfWorkJournal
import io.github.crabzilla.pgc.cleanDatabase
import io.github.crabzilla.pgc.example1.Example1Fixture.CUSTOMER_ENTITY
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
import io.vertx.core.Handler
import io.vertx.core.Vertx
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

  private lateinit var writeDb: PgPool
  private lateinit var journal: UnitOfWorkJournal

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
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
      journal = PgcUowJournal(writeDb, example1Json)
      cleanDatabase(vertx, config)
        .onSuccess {
          tc.completeNow()
          println("ok")
        }
        .onFailure { err ->
          tc.failNow(err)
          err.printStackTrace()
        }
    })
  }

  @Test
  @DisplayName("can append version 1")
  fun s1(tc: VertxTestContext) {
    journal.append(createdUow1)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("cannot append version 1 twice")
  fun s2(tc: VertxTestContext) {
    journal.append(createdUow1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        journal.append(createdUow1)
          .onSuccess { tc.failNow("should fail") }
          .onFailure { err ->
            tc.verify { assertThat(err.message).isEqualTo("expected version is 0 but current version is 1") }
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("cannot append version 3 after version 1")
  fun s22(tc: VertxTestContext) {
    val createdUow3 =
      UnitOfWork(CUSTOMER_ENTITY, customerId1, UUID.randomUUID(), createCmd1, 3, listOf(created1))
    // append uow1
    journal.append(createdUow1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        journal.append(createdUow3)
          .onSuccess { tc.failNow("should fail") }
          .onFailure { err ->
            tc.verify { assertThat(err.message).isEqualTo("expected version is 2 but current version is 1") }
            tc.completeNow()
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
        journal.append(activatedUow1).onComplete { ar2 ->
          if (ar2.failed()) {
            tc.failNow(ar2.cause())
          } else {
            // TODO check db state
            tc.completeNow()
          }
        }
      }
    }
  }
}
