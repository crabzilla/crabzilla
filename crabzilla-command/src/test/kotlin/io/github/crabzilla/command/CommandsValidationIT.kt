package io.github.crabzilla.command

import io.github.crabzilla.TestRepository
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.customerModule
import io.github.crabzilla.pgPool
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
class CommandsValidationIT {

  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    commandController = CommandControllerBuilder(vertx, pgPool)
      .build(customerModule, customerConfig)
    testRepo = TestRepository(pgPool)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can validate before command handler`(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "bad customer")
    val metadata = CommandMetadata.new(id)
    commandController.handle(metadata, cmd)
      .onFailure {
        assertEquals(it.message, "[Bad customer!]")
        tc.completeNow()
      }
      .onSuccess {
        tc.failNow("It should fail")
      }
  }

  @Test
  fun `it can validate within command handler`(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata.new(id)
    commandController.handle(metadata, cmd)
      .compose {
        commandController.handle(
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
