package io.github.crabzilla.command

import io.github.crabzilla.TestsFixtures.json
import io.github.crabzilla.TestsFixtures.pgPool
import io.github.crabzilla.example1.customer.CustomersEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.vertx.core.Vertx
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Instantiating a command controller")
internal class CommandControllerTest {

  val vertx: Vertx = Vertx.vertx()

  @Test
  fun `a command controller can be created`() {
    val controller = CommandController(vertx, pgPool, json, customerConfig)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created with a custom synchronous event projector`() {
    val options = CommandControllerOptions(eventsProjector = CustomersEventsProjector("customers"))
    val controller = CommandController(vertx, pgPool, json, customerConfig, options)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be be customized with the event stream size`() {
    val options = CommandControllerOptions(eventStreamSize = 1)
    val controller = CommandController(vertx, pgPool, json, customerConfig, options)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can start notifying to postgres`() {
    val options = CommandControllerOptions(eventsProjector = CustomersEventsProjector("customers"))
    val controller = CommandController(vertx, pgPool, json, customerConfig, options).startPgNotification()
    assertNotNull(controller) // TODO assert on postgres subscriber latch
  }

  @Test
  fun `a command controller can be customized to publish to eventbus`() {
    val options = CommandControllerOptions(publishToEventBus = true)
    val controller = CommandController(vertx, pgPool, json, customerConfig, options)
    assertNotNull(controller) // TODO assert on eventbus latch
  }
}
