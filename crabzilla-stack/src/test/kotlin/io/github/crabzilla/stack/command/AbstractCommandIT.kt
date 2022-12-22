package io.github.crabzilla.stack.command

import io.github.crabzilla.TestRepository
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.ulidFunction
import io.github.crabzilla.stack.CrabzillaContext
import io.github.crabzilla.stack.DefaultCrabzillaContextFactory
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach

open class AbstractCommandIT {

  lateinit var context : CrabzillaContext
  lateinit var factory : CommandServiceApiFactory
  lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = DefaultCrabzillaContextFactory().new(vertx, testDbConfig, ulidFunction)
    factory = DefaultCommandServiceApiFactory(context)
    testRepo = TestRepository(context.pgPool())
    cleanDatabase(context.pgPool())
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

}
