package io.github.crabzilla.stack.command

import io.github.crabzilla.TestRepository
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.stack.CrabzillaVertxContext
import io.github.crabzilla.testDbConfig
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

  private lateinit var context : CrabzillaVertxContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaVertxContext.new(vertx, testDbConfig)
    testRepo = TestRepository(context.pgPool())
    cleanDatabase(context.pgPool())
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can publish to eventbus`(vertx: Vertx, tc: VertxTestContext) {
    val options = CommandServiceOptions(eventBusTopic = "MY_TOPIC")
    val service = context.commandService(customerComponent, jsonSerDer, options)

    val jsonMessage = AtomicReference<JsonObject>()
    val latch = CountDownLatch(1)
    vertx.eventBus().consumer<JsonObject>("MY_TOPIC") { msg ->
      jsonMessage.set(msg.body())
      latch.countDown()
    }
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    service.handle(id, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        tc.verify {
          latch.await(2, TimeUnit.SECONDS)
          assertTrue(jsonMessage.get().getJsonArray("events").size() == 2)
          tc.completeNow()
        }
      }
  }
}
