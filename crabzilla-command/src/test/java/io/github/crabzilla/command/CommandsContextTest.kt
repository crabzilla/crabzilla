package io.github.crabzilla.command

import io.github.crabzilla.example1.customer.CustomerEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.vertx.core.Vertx
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class CommandsContextTest {

  @Test
  fun create() {
    val jsonSerDer = KotlinJsonSerDer(example1Json)
    val commandsContext = CommandsContext.create(Vertx.vertx(), jsonSerDer, connectOptions, poolOptions)
    val controller = commandsContext.create(customerConfig, SnapshotType.PERSISTENT)
    assertNotNull(controller)
  }

  @Test
  fun testCreate() {
    val jsonSerDer = KotlinJsonSerDer(example1Json)
    val commandsContext = CommandsContext.create(Vertx.vertx(), jsonSerDer, connectOptions, poolOptions)
    val controller = commandsContext
      .create(customerConfig, SnapshotType.ON_DEMAND, CustomerEventsProjector)
    assertNotNull(controller)
  }
}
