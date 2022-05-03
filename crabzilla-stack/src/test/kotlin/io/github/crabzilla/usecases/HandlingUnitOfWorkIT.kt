package io.github.crabzilla.usecases

import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.TestRepository
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.command.CommandMetadata
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
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
@DisplayName("Handling Unit Of Work")
class HandlingUnitOfWorkIT {

  private lateinit var context: CrabzillaContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaContext.new(vertx, testDbConfig)
    testRepo = TestRepository(context.pgPool)
    cleanDatabase(context.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  // https://martinfowler.com/eaaCatalog/unitOfWork.html

  @Test
  fun `it can handle 2 commands within more than 1 instances of the same state`(vertx: Vertx, tc: VertxTestContext) {
    val options = FeatureOptions(eventBusTopic = "MY_TOPIC", pgNotificationInterval = 100)
    val controller = context.commandController(customerComponent, jsonSerDer, options)
    val latch = CountDownLatch(2)
    val stateTypeMsg = AtomicReference(mutableListOf<JsonObject>())
    vertx.eventBus().consumer<JsonObject>("MY_TOPIC") { msg ->
      stateTypeMsg.get().add(msg.body())
      latch.countDown()
      msg.reply(null)
    }
    val id = UUID.randomUUID(); val metadata = CommandMetadata.new(id)
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, "c1", "is needed")
    val id2 = UUID.randomUUID(); val metadata2 = CommandMetadata.new(id2)
    val cmd2 = CustomerCommand.RegisterAndActivateCustomer(id2, "c2", "is needed")
    controller
      .withinTransaction {
        controller.handle(metadata, cmd)
          .compose { controller.handle(metadata2, cmd2) }
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        vertx.executeBlocking<Void> {
          tc.verify {
            latch.await(2, TimeUnit.SECONDS)
            assertEquals(2, stateTypeMsg.get().size)
            it.complete()
          }
        }.onSuccess {
          testRepo.getAllCommands()
            .onFailure { tc.failNow(it) }
            .onSuccess { list ->
                tc.verify {
                  assertEquals(2, list.size)
                  tc.completeNow()
                }
            }
        }.onFailure {
          tc.failNow(it)
        }
      }
   }
}
