package io.github.crabzilla.stack.command

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
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
@DisplayName("Handling Unit Of Work")
class HandlingUnitOfWorkIT: AbstractCommandIT() {

  // https://martinfowler.com/eaaCatalog/unitOfWork.html

  @Test
  fun `it can handle 2 commands within a transaction`(vertx: Vertx, tc: VertxTestContext) {
    val options = CommandServiceOptions(eventBusTopic = "MY_TOPIC")
    val service = factory.commandService(customerConfig, jsonSerDer, options)
    val latch = CountDownLatch(4)
    val stateTypeMsg = AtomicReference(mutableListOf<JsonObject>())
    vertx.eventBus().consumer<JsonObject>("MY_TOPIC") { msg ->
      stateTypeMsg.get().add(msg.body())
      latch.countDown()
      msg.reply(null)
    }
    val id = UUID.randomUUID().toString()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val id2 = UUID.randomUUID().toString()
    val cmd2 = RegisterAndActivateCustomer(id2, "c2", "is needed")
    service
      .withinTransaction { tx ->
        service.handle(tx, id, cmd)
          .compose { service.handle(tx, id2, cmd2) }
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
          testRepo.scanEvents(0, 1000)
            .onFailure { tc.failNow(it) }
            .onSuccess { list ->
                tc.verify {
                  assertEquals(4, list.size)
                  tc.completeNow()
                }
            }
        }.onFailure {
          tc.failNow(it)
        }
      }
   }

  @Test
  fun `it can rollback when handling 2 commands within a transaction `(vertx: Vertx, tc: VertxTestContext) {
    val service = factory.commandService(customerConfig, jsonSerDer)
    val id = UUID.randomUUID().toString()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val cmd2 = RegisterAndActivateCustomer(id, "c1", "is needed")
    service
      .withinTransaction { tx ->
        service.handle(tx, id, cmd)
          .compose { service.handle(tx, id, cmd2) }
          .onComplete {
            testRepo.scanEvents(0, 1000)
              .onFailure { tc.failNow(it) }
              .onSuccess { list ->
                tc.verify {
                  assertEquals(0, list.size)
                  tc.completeNow()
                }
              }
          }
        }
      }
}
