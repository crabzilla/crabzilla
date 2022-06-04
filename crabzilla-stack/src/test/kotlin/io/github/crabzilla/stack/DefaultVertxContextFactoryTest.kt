package io.github.crabzilla.stack

import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx.vertx
import org.junit.jupiter.api.Test

internal class DefaultVertxContextFactoryTest {

  @Test
  fun `it can be created without pgpool`() {
    val context = DefaultVertxContextFactory().new(vertx(), testDbConfig)
  }

  @Test
  fun `it can be created with pgpool`() {
    val context = DefaultVertxContextFactory().new(vertx(), testDbConfig)
    val context2 = DefaultVertxContextFactory().new(vertx(), testDbConfig, context.pgPool())
  }

}
