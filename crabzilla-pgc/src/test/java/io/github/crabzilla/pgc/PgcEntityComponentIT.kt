package io.github.crabzilla.pgc

import io.github.crabzilla.CommandMetadata
import io.github.crabzilla.EntityComponent
import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.CustomerId
import io.github.crabzilla.example1.UnknownCommand
import io.github.crabzilla.example1.aggregate.Customer
import io.github.crabzilla.initVertx
import io.github.crabzilla.pgc.example1.Example1Fixture.customerPgcComponent
import io.reactiverse.pgclient.PgPool
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
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
class PgcEntityComponentIT {

  private lateinit var vertx: Vertx
  private lateinit var writeDb: PgPool
  private lateinit var customerComponent: EntityComponent<Customer>

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    vertx = Vertx.vertx()

    initVertx(vertx)

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))

    val retriever = ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(envOptions))

    retriever.getConfig(Handler { configFuture ->

      if (configFuture.failed()) {
        println("Failed to get configuration")
        tc.failNow(configFuture.cause())
        return@Handler
      }

      val config = configFuture.result()

      writeDb = pgPool(vertx, "WRITE", config)

      customerComponent = customerPgcComponent(vertx, writeDb)

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
          tc.completeNow()
        }
      }

    })

  }

  @Test
  @DisplayName("given a valid command it will be SUCCESS")
  fun a1(tc: VertxTestContext) {

    val customerId = CustomerId(1)
    val command = CreateCustomer("customer1")
    val commandMetadata = CommandMetadata(customerId.value, "create")

    customerComponent.handleCommand(commandMetadata, command, Handler { event ->
      if (event.succeeded()) {
        val result = event.result()
        println(result)
        tc.verify { assertThat(result.first.events.size).isEqualTo(1) }
        tc.completeNow()
      } else {
        tc.failNow(event.cause())
      }
    })

  }

  @Test
  @DisplayName("given an invalid command it will be VALIDATION_ERROR")
  fun a2(tc: VertxTestContext) {
    val customerId = CustomerId(1)
    val command = CreateCustomer("a bad name")
    val commandMetadata = CommandMetadata(customerId.value, "create")

    customerComponent.handleCommand(commandMetadata, command, Handler { event ->
      tc.verify { assertThat(event.succeeded()).isFalse() }
      tc.verify { assertThat(event.cause().message).isEqualTo("[Invalid name: a bad name]") }
      tc.completeNow()
    })

  }

  @Test
  @DisplayName("given an execution error it will be HANDLING_ERROR")
  fun a3(tc: VertxTestContext) {
    val customerId = CustomerId(1)
    val commandMetadata = CommandMetadata(customerId.value, "unknown")
    val command = UnknownCommand(customerId)

    customerComponent.handleCommand(commandMetadata, command, Handler { event ->
      tc.verify { assertThat(event.succeeded()).isFalse() }
      tc.verify { assertThat(event.cause().message).isEqualTo("unknown is a unknown command") }
      tc.completeNow()
    })

  }

}
