package io.github.crabzilla.stack

import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx.vertx
import org.junit.jupiter.api.Test

internal class DefaultCrabzillaContextFactoryTest {

  @Test
  fun `it can be created without pgpool`() {
    val context = DefaultCrabzillaContextFactory().new(vertx(), testDbConfig)
  }

  @Test
  fun `it can be created with pgpool`() {
    val context = DefaultCrabzillaContextFactory().new(vertx(), testDbConfig)
    val context2 = DefaultCrabzillaContextFactory().new(vertx(), testDbConfig, context.pgPool())
  }

}
