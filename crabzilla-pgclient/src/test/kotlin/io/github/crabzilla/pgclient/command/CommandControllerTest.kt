package io.github.crabzilla.pgclient.command

import io.github.crabzilla.example1.customer.CustomersEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.customerModule
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Instantiating a command controller")
internal class CommandControllerTest {

  @Test
  fun `a command controller can be created`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
        .build(customerModule, customerConfig)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created with a custom synchronous event projector`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(customerModule, customerConfig, CustomersEventsProjector("customers"))
    assertNotNull(controller)
  }

}
