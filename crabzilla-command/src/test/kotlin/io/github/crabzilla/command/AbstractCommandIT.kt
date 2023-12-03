package io.github.crabzilla.command

import CrabzillaContext
import DefaultCrabzillaContextFactory
import io.github.crabzilla.TestRepository
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import ulid4j.Ulid

@ExtendWith(VertxExtension::class)
open class AbstractCommandIT {
  lateinit var context: CrabzillaContext
  lateinit var commandComponent: CommandComponent<CustomerCommand>
  lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    context = DefaultCrabzillaContextFactory().new(vertx, testDbConfig, ulidFunction)
    commandComponent = DefaultCommandComponent(context, customerConfig)
    testRepo = TestRepository(context.pgPool())

    cleanDatabase(context.pgPool())
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  companion object {
    val ulidGenerator = Ulid()
    val ulidFunction = { ulidGenerator.next() }
  }
}
