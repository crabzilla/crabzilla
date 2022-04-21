package io.github.crabzilla.command

import io.github.crabzilla.connectOptions
import io.github.crabzilla.example1.customer.CustomersEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.objectMapper
import io.github.crabzilla.poolOptions
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
      .build(objectMapper, customerConfig)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created with a custom synchronous event projector`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(objectMapper, customerConfig, CustomersEventsProjector("customers"))
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be be customized with the event stream size`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(objectMapper, customerConfig)
    controller.eventStreamSize = 10
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can start notifying to postgres`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(objectMapper, customerConfig)
    controller.startPgNotification(30_000L)
    assertNotNull(controller)
  }
}
