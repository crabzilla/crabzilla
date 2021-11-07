package io.github.crabzilla.engine.command

import io.github.crabzilla.core.json.KotlinJsonSerDer
import io.github.crabzilla.engine.command.SnapshotType.ON_DEMAND
import io.github.crabzilla.engine.command.SnapshotType.PERSISTENT
import io.github.crabzilla.engine.connectOptions
import io.github.crabzilla.engine.poolOptions
import io.github.crabzilla.example1.customer.CustomerEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.vertx.core.Vertx
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class CommandsContextTest {

  @Test
  fun create() {
    val jsonSerDer = KotlinJsonSerDer(example1Json)
    val commandsContext = CommandsContext.create(Vertx.vertx(), jsonSerDer, connectOptions, poolOptions)
    val controller = commandsContext.create(customerConfig, PERSISTENT)
    assertNotNull(controller)
  }

  @Test
  fun testCreate() {
    val jsonSerDer = KotlinJsonSerDer(example1Json)
    val commandsContext = CommandsContext.create(Vertx.vertx(), jsonSerDer, connectOptions, poolOptions)
    val controller = commandsContext
      .create(customerConfig, ON_DEMAND, CustomerEventsProjector)
    assertNotNull(controller)
  }
}
