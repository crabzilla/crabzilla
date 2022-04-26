package io.github.crabzilla.command

import io.github.crabzilla.Jackson.json
import io.github.crabzilla.example1.customer.CustomersPgEventProjector
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.pgPool
import io.github.crabzilla.stack.CommandControllerOptions
import io.vertx.core.Vertx
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Instantiating a command controller")
internal class CommandControllerTest {

  val vertx: Vertx = Vertx.vertx()

  @Test
  fun `a command controller can be created`() {
    val controller = CommandController(vertx, pgPool, json, customerComponent)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created and start via factory`() {
    val controller = CommandController.createAndStart(vertx, pgPool, json, customerComponent)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created with a custom synchronous event projector`() {
    val options = CommandControllerOptions(pgEventProjector = CustomersPgEventProjector())
    val controller = CommandController(vertx, pgPool, json, customerComponent, options)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be be customized with the event stream size`() {
    val options = CommandControllerOptions(eventStreamSize = 1)
    val controller = CommandController(vertx, pgPool, json, customerComponent, options)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can start notifying to postgres`() {
    val options = CommandControllerOptions(pgEventProjector = CustomersPgEventProjector())
    val controller = CommandController(vertx, pgPool, json, customerComponent, options).startPgNotification()
    assertNotNull(controller) // TODO assert on postgres subscriber latch
  }

  @Test
  fun `a command controller can be customized to publish to eventbus`() {
    val options = CommandControllerOptions(eventBusTopic = "MY-TOPIC")
    val controller = CommandController(vertx, pgPool, json, customerComponent, options)
    assertNotNull(controller) // TODO assert on eventbus latch
  }
}
