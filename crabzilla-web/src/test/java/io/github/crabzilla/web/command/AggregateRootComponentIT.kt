package io.github.crabzilla.web.command

import io.github.crabzilla.core.command.CommandMetadata
import io.github.crabzilla.core.command.CrabzillaContext
import io.github.crabzilla.core.command.SnapshotRepository
import io.github.crabzilla.pgc.command.PgcSnapshotRepo
import io.github.crabzilla.pgc.command.PgcUowJournal
import io.github.crabzilla.pgc.command.PgcUowJournal.FullPayloadPublisher
import io.github.crabzilla.pgc.command.PgcUowRepo
import io.github.crabzilla.web.boilerplate.PgClientSupport.writeModelPgPool
import io.github.crabzilla.web.example1.CreateCustomer
import io.github.crabzilla.web.example1.Customer
import io.github.crabzilla.web.example1.CustomerCommandAware
import io.github.crabzilla.web.example1.Example1Fixture
import io.github.crabzilla.web.example1.Example1Fixture.createActivateCmd1
import io.github.crabzilla.web.example1.cleanDatabase
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
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

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregateRootComponentIT {

  companion object {
    private lateinit var vertx: Vertx
    private lateinit var writeDb: PgPool
    private lateinit var customerHandler: AggregateRootWebHandler<Customer>
    val CUSTOMER_HANDLER: (vertx: Vertx, writeDb: PgPool) -> AggregateRootWebHandler<Customer> =
      { vertx: Vertx, writeDb: PgPool ->
        val cmdAware = CustomerCommandAware()
        val uowRepo = PgcUowRepo(writeDb, Example1Fixture.example1Json)
        val uowJournal = PgcUowJournal(writeDb, Example1Fixture.example1Json, FullPayloadPublisher(vertx))
        val snapshotRepo:
                SnapshotRepository<Customer> = PgcSnapshotRepo(writeDb, Example1Fixture.example1Json, CustomerCommandAware())
        val ctx = CrabzillaContext(Example1Fixture.example1Json, uowRepo, uowJournal)
        AggregateRootWebHandler(ctx, snapshotRepo, cmdAware)
      }
  }

  @BeforeEach
  fun setup(tc: VertxTestContext) {
    vertx = Vertx.vertx()
    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))
    val retriever = ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(envOptions))
    retriever.getConfig(Handler { configFuture ->
      if (configFuture.failed()) {
        tc.failNow(configFuture.cause())
        return@Handler
      }
      val config = configFuture.result()
      writeDb = writeModelPgPool(vertx, config)
      customerHandler = CUSTOMER_HANDLER(vertx, writeDb)
      cleanDatabase(vertx, config)
        .onSuccess { tc.completeNow() }
        .onFailure { tc.failNow(it) }
    })
  }

  // via handleCommand

  @Test
  @DisplayName("given a valid command it will be SUCCESS")
  fun a1(tc: VertxTestContext) {
    val customerId = 1
    val command = CreateCustomer("customer1")
    val commandMetadata = CommandMetadata(customerId, "customer", "create")
    customerHandler.handleCommand(commandMetadata, command).onComplete { event ->
      if (event.succeeded()) {
        val result = event.result()
        tc.verify { assertThat(result.first.events.size).isEqualTo(1) }
        tc.completeNow()
      } else {
        tc.failNow(event.cause())
      }
    }
  }

  @Test
  @DisplayName("given an invalid command it will be VALIDATION_ERROR")
  fun a2(tc: VertxTestContext) {
    val customerId = 1
    val command = CreateCustomer("a bad name")
    val commandMetadata = CommandMetadata(customerId, "customer", "create")
    customerHandler.handleCommand(commandMetadata, command).onComplete { event ->
      tc.verify { assertThat(event.succeeded()).isFalse() }
      tc.verify { assertThat(event.cause().message).isEqualTo("[Invalid name: a bad name]") }
      tc.completeNow()
    }
  }

  // createActivateCmd1

  @Test
  @DisplayName("given a valid composed command it will be SUCCESS")
  fun a4(tc: VertxTestContext) {
    val customerId = 1
    val commandMetadata = CommandMetadata(customerId, "customer", "create")
    val command = createActivateCmd1
    customerHandler.handleCommand(commandMetadata, command).onComplete { event ->
      if (event.succeeded()) {
        val result = event.result()
        tc.verify { assertThat(result.first.events.size).isEqualTo(2) }
        tc.completeNow()
      } else {
        tc.failNow(event.cause())
      }
    }
  }
}