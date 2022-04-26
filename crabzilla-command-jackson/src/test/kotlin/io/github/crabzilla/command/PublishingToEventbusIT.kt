package io.github.crabzilla.command

import io.github.crabzilla.Jackson.json
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.pgPool
import io.github.crabzilla.stack.CommandControllerOptions
import io.github.crabzilla.stack.CommandMetadata
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
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Publishing to eventbus")
class PublishingToEventbusIT {

  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    commandController = CommandController(vertx, pgPool, json, customerComponent)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can publish to eventbus`(vertx: Vertx, tc: VertxTestContext) {
    val options = CommandControllerOptions(eventBusTopic = "MY_TOPIC")
    val controller = CommandController.createAndStart(vertx, pgPool, json, customerComponent, options)
    val jsonMessage = AtomicReference<JsonObject>()
    val latch = CountDownLatch(1)
    vertx.eventBus().consumer<JsonObject>("MY_TOPIC") { msg ->
      latch.countDown()
      println(msg.body().encodePrettily())
      jsonMessage.set(msg.body())
    }
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata.new(id)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          latch.await()
          assertTrue(jsonMessage.get().getJsonArray("events").size() == 2)
          tc.completeNow()
        }
      }
  }
}
