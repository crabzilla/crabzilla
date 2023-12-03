package io.github.crabzilla

import DefaultCrabzillaContextFactory
import io.vertx.core.Vertx.vertx
import org.junit.jupiter.api.Test
import ulid4j.Ulid

internal class ContextFactoryTest {
  @Test
  fun `it can be created without pgpool`() {
    val context = DefaultCrabzillaContextFactory().new(vertx(), testDbConfig, ulidFunction)
  }

  @Test
  fun `it can be created with pgpool`() {
    val context = DefaultCrabzillaContextFactory().new(vertx(), testDbConfig, ulidFunction)
    val context2 = DefaultCrabzillaContextFactory().new(vertx(), testDbConfig, context.pgPool(), ulidFunction)
  }

  companion object {
    val ulidGenerator = Ulid()
    val ulidFunction = { ulidGenerator.next() }
  }
}
