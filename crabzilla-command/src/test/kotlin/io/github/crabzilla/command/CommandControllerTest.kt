package io.github.crabzilla.command

import io.github.crabzilla.PgConnectOptionsFactory.from
import io.github.crabzilla.dbConfig
import io.github.crabzilla.example1.customer.CustomersEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.customerModule
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Instantiating a command controller")
internal class CommandControllerTest {

  val vertx: Vertx = Vertx.vertx()
  val pgPool: PgPool = PgPool.pool(from(dbConfig), PoolOptions())

  @Test
  fun `a command controller can be created`() {
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(customerModule, customerConfig)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created with a custom synchronous event projector`() {
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(customerModule, customerConfig, CustomersEventsProjector("customers"))
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be be customized with the event stream size`() {
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(customerModule, customerConfig)
    controller.eventStreamSize = 10
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can start notifying to postgres`() {
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(customerModule, customerConfig)
    controller.startPgNotification(30_000L)
    assertNotNull(controller)
  }
}
