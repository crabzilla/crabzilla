package io.github.crabzilla.stack.command

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerComponent
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
@DisplayName("Validating commands")
class ValidatingCommandIT: AbstractCommandIT() {

  @Test
  fun `it can validate before command handler`(tc: VertxTestContext, vertx: Vertx) {

    val options = CommandServiceOptions(eventBusTopic = "MY_TOPIC")
    val service = factory.commandService(customerComponent, jsonSerDer, options)

    val id = UUID.randomUUID()
    val cmd = RegisterCustomer(id, "bad customer")
    service.handle(id, cmd)
      .onFailure {
        assertEquals(it.message, "[Bad customer!]")
        tc.completeNow()
      }
      .onSuccess {
        tc.failNow("It should fail")
      }
  }

  @Test
  fun `it can validate within command handler`(tc: VertxTestContext, vertx: Vertx) {

    val options = CommandServiceOptions(eventBusTopic = "MY_TOPIC")
    val service = factory.commandService(customerComponent, jsonSerDer, options)

    val id = UUID.randomUUID()
    val cmd = RegisterCustomer(id, "good customer")
    service.handle(id, cmd)
      .compose {
        service.handle(id, ActivateCustomer("because I want it"))
      }
      .onFailure {
        tc.completeNow()
      }
      .onSuccess {
        tc.failNow("It should fail")
      }
  }
}
