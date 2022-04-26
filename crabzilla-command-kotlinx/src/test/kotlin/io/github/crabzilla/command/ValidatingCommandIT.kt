package io.github.crabzilla.command

import io.github.crabzilla.TestsFixtures
import io.github.crabzilla.TestsFixtures.pgPool
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Validating commands")
class ValidatingCommandIT {

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can validate before command handler`(tc: VertxTestContext, vertx: Vertx) {

    val repository = KotlinxCommandRepository(TestsFixtures.json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, repository)

    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "bad customer")
    val metadata = CommandMetadata.new(id)
    controller.handle(metadata, cmd)
      .onFailure {
        assertEquals(it.message, "[Bad customer!]")
        tc.completeNow()
      }
      .onSuccess {
        tc.failNow("It should fail")
      }
  }

  @Test
  fun `it can validate within command handler`(tc: VertxTestContext, vertx: Vertx) {

    val repository = KotlinxCommandRepository(TestsFixtures.json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, repository)

    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata.new(id)
    controller.handle(metadata, cmd)
      .compose {
        controller.handle(
          CommandMetadata.new(id),
          CustomerCommand.ActivateCustomer("because I want it")
        )
      }
      .onFailure {
        tc.completeNow()
      }
      .onSuccess {
        tc.failNow("It should fail")
      }
  }
}
