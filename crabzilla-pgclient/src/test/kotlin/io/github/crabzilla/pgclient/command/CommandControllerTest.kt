package io.github.crabzilla.pgclient.command

import io.github.crabzilla.example1.customer.CustomersEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.example1Json
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Instantiating a command controller")
internal class CommandControllerTest {

  @Test
  fun `a command controller can be created with SnapshotType ON_DEMAND`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
        .build(example1Json, customerConfig, SnapshotType.ON_DEMAND)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created with SnapshotType PERSISTENT`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(example1Json, customerConfig, SnapshotType.PERSISTENT)
    assertNotNull(controller)
  }

  @Test
  fun `a command controller can be created with a custom synchronous event projector`() {
    val vertx = Vertx.vertx()
    val pgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    val controller = CommandControllerBuilder(vertx, pgPool)
      .build(example1Json, customerConfig, SnapshotType.ON_DEMAND, CustomersEventsProjector("customers"))
    assertNotNull(controller)
  }

}
