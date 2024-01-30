package io.github.crabzilla.handler

import io.github.crabzilla.TestRepository
import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
open class AbstractCommandIT {
  lateinit var context: CrabzillaContext
  lateinit var crabzillaHandler: CrabzillaHandler<CustomerCommand>
  lateinit var testRepository: TestRepository

  @BeforeEach
  fun setup(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    context = CrabzillaContextImpl(vertx, TestRepository.testDbConfig)
    crabzillaHandler = CrabzillaHandlerImpl(context, customerConfig)
    testRepository = TestRepository(context.pgPool)

    TestRepository.cleanDatabase(context.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }
}
