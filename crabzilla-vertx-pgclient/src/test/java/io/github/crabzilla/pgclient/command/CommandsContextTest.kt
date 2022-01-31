package io.github.crabzilla.pgclient.command

import io.github.crabzilla.example1.customer.CustomersEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class CommandsContextTest {

  @Test
  fun create() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val jsonSerDer = KotlinJsonSerDer(example1Json)
    val commandsContext = CommandsContext(vertx, jsonSerDer, pgPool)
    val controller = commandsContext.create(customerConfig, SnapshotType.PERSISTENT)
    assertNotNull(controller)
  }

  @Test
  fun testCreate() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val jsonSerDer = KotlinJsonSerDer(example1Json)
    val commandsContext = CommandsContext(vertx, jsonSerDer, pgPool)
    val controller = commandsContext
      .create(customerConfig, SnapshotType.ON_DEMAND, CustomersEventsProjector("customers"))
    assertNotNull(controller)
  }
}
