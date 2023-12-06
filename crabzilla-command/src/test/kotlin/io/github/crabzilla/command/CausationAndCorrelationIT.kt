package io.github.crabzilla.command

import io.github.crabzilla.example1.customer.CustomerCommand
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
@DisplayName("Persisting events - causation and correlation")
class CausationAndCorrelationIT : AbstractCommandIT() {
  @Test
  fun `when handling handle 4 commands, causation and correlation are correct`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val id = UUID.randomUUID().toString()
    val cmd1 = CustomerCommand.RegisterCustomer(id, "customer1")
    val cmd2 = CustomerCommand.ActivateCustomer("is needed")
    val cmd3 = CustomerCommand.DeactivateCustomer("is not needed anymore")
    val cmd4 = CustomerCommand.ActivateCustomer("is needed again")
    commandComponent.handle(id, cmd1)
      .compose { commandComponent.handle(id, cmd2) }
      .compose { commandComponent.handle(id, cmd3) }
      .compose { commandComponent.handle(id, cmd4) }
//    handler
//      .withinTransaction { tx ->
//        handler.handle(tx, id, cmd1)
//          .compose { handler.handle(tx, id, cmd2) }
//          .compose { handler.handle(tx, id, cmd3) }
//          .compose { handler.handle(tx, id, cmd4) }
//      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.scanEvents(0, 1000)
          .onFailure { tc.failNow(it) }
          .onSuccess { list ->
            list.forEach {
              println(it)
            }
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
