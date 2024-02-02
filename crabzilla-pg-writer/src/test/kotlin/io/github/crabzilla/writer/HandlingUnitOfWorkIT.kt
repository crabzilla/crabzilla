package io.github.crabzilla.writer

import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Handling Unit Of Work")
class HandlingUnitOfWorkIT : AbstractCrabzillaWriterIT() {
  // https://martinfowler.com/eaaCatalog/unitOfWork.html
  // https://stackoverflow.com/questions/49288197/stream-aggregate-relationship-in-an-event-sourced-system

  @Test
  fun `it can handle 2 commands within a transaction`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "c1", "is needed")

    val customerId2 = UUID.randomUUID()
    val targetStream2 = TargetStream(stateType = "Customer", stateId = customerId2.toString())
    val cmd2 = RegisterAndActivateCustomer(customerId2, "c2", "is needed")

    context
      .withinTransaction { tx ->
        crabzillaWriter.handleWithinTransaction(tx, targetStream1, cmd1)
          .compose { crabzillaWriter.handleWithinTransaction(tx, targetStream2, cmd2) }
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepository.getEvents(0, 1000)
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

  @Test
  fun `it can rollback when handling 2 commands within a transaction `(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    val cmd2 = RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    context
      .withinTransaction { tx ->
        crabzillaWriter.handleWithinTransaction(tx, targetStream1, cmd1)
          .compose { crabzillaWriter.handleWithinTransaction(tx, targetStream1, cmd2) }
          .onComplete {
            testRepository.getEvents(0, 1000)
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
