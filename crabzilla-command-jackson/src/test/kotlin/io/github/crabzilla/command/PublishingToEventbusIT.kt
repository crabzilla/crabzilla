package io.github.crabzilla.command

import io.github.crabzilla.Jackson.json
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.pgPool
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Publishing to eventbus")
class PublishingToEventbusIT {

  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    commandController = CommandController(vertx, pgPool, json, customerConfig)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can publish to eventbus`(vertx: Vertx, tc: VertxTestContext) {
    val latch = CountDownLatch(1)
    val options = CommandControllerOptions(publishToEventBus = true)
    val controller = CommandController.createAndStart(vertx, pgPool, json, customerConfig, options)
    val jsonMessage = AtomicReference<JsonObject>()
    vertx.eventBus().consumer<JsonObject>(customerConfig.stateClassName()) { msg ->
      latch.countDown()
      jsonMessage.set(msg.body())
    }
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata.new(id)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          assertTrue(latch.await(1, TimeUnit.SECONDS))
          assertTrue(jsonMessage.get().getJsonArray("events").size() == 2)
          tc.completeNow()
        }
      }
  }
}
