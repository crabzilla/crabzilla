package io.github.crabzilla

import DefaultCrabzillaContextFactory
import io.vertx.core.Vertx.vertx
import org.junit.jupiter.api.Test

internal class ContextFactoryTest {
  @Test
  fun `it can be created without pgpool`() {
    DefaultCrabzillaContextFactory().new(vertx(), testDbConfig)
  }

  @Test
  fun `it can be created with pgpool`() {
    val context = DefaultCrabzillaContextFactory().new(vertx(), testDbConfig)
    DefaultCrabzillaContextFactory().new(vertx(), testDbConfig, context.pgPool())
  }
}
