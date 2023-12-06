package io.github.crabzilla.command

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
class HandlingUnitOfWorkIT : AbstractCommandIT() {
  // https://martinfowler.com/eaaCatalog/unitOfWork.html

  @Test
  fun `it can handle 2 commands within a transaction`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val id1 = UUID.randomUUID().toString()
    val cmd1 = RegisterAndActivateCustomer(id1, "c1", "is needed")
    val id2 = UUID.randomUUID().toString()
    val cmd2 = RegisterAndActivateCustomer(id2, "c2", "is needed")
    commandComponent
      .withinTransaction { tx ->
        commandComponent.handle(tx, id1, cmd1)
          .compose { commandComponent.handle(tx, id2, cmd2) }
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
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

  @Test
  fun `it can rollback when handling 2 commands within a transaction `(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val id = UUID.randomUUID().toString()
    val cmd1 = RegisterAndActivateCustomer(id, "c1", "is needed")
    val cmd2 = RegisterAndActivateCustomer(id, "c1", "is needed")
    commandComponent
      .withinTransaction { tx ->
        commandComponent.handle(tx, id, cmd1)
          .compose { commandComponent.handle(tx, id, cmd2) }
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
