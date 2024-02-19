package io.github.crabzilla.writer

import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.stream.TargetStream
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Publishing to eventbus")
@Disabled
class PublishingToEventbusIT : AbstractCommandHandlerIT() {
  @Test
  fun `it can publish to eventbus`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val commander = CommandHandlerImpl(context, customerConfig)
    val jsonMessage = AtomicReference<JsonObject>()
    val latch = CountDownLatch(2)
    vertx.eventBus().consumer<JsonObject>("MY_TOPIC") { msg ->
      jsonMessage.set(msg.body())
      latch.countDown()
    }
    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val command = RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    commander.handle(targetStream1, command)
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
