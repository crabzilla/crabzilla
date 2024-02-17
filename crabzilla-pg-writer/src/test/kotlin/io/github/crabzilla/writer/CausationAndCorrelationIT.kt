package io.github.crabzilla.writer

import io.github.crabzilla.example1.customer.model.CustomerCommand
import io.github.crabzilla.stream.TargetStream
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
@DisplayName("Causation and correlation")
class CausationAndCorrelationIT : AbstractWriterApiIT() {
  @Test
  fun `when handling handle 4 commands, causation and correlation are correct`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerId = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId.toString())
    val cmd1 = CustomerCommand.RegisterCustomer(customerId, "customer1")
    val cmd2 = CustomerCommand.ActivateCustomer("is needed")
    val cmd3 = CustomerCommand.DeactivateCustomer("is not needed anymore")
    val cmd4 = CustomerCommand.ActivateCustomer("is needed again")
    writerApi.handle(targetStream, cmd1)
      .compose { writerApi.handle(targetStream, cmd2) }
      .compose { writerApi.handle(targetStream, cmd3) }
      .compose { writerApi.handle(targetStream, cmd4) }
//    handler
//      .withinTransaction { tx ->
//        handler.handle(tx, id, cmd1)
//          .compose { handler.handle(tx, id, cmd2) }
//          .compose { handler.handle(tx, id, cmd3) }
//          .compose { handler.handle(tx, id, cmd4) }
//      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepository.getEvents(0, 1000)
          .onFailure { tc.failNow(it) }
          .onSuccess { list ->
            tc.verify {
              assertEquals(4, list.size)

              assertEquals(list[0].getString("causation_id"), list[0].getString("id"))
              assertEquals(list[0].getString("correlation_id"), list[0].getString("id"))

              assertEquals(list[1].getString("causation_id"), list[0].getString("id"))
              assertEquals(list[1].getString("correlation_id"), list[0].getString("correlation_id"))

              assertEquals(list[2].getString("causation_id"), list[1].getString("id"))
              assertEquals(list[2].getString("correlation_id"), list[0].getString("correlation_id"))

              assertEquals(list[3].getString("causation_id"), list[2].getString("id"))
              assertEquals(list[3].getString("correlation_id"), list[0].getString("correlation_id"))
              tc.completeNow()
            }
          }
      }.onFailure {
        tc.failNow(it)
      }
  }
}
