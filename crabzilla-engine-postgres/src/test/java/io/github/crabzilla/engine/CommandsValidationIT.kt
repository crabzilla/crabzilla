package io.github.crabzilla.engine

import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.core.serder.KotlinJsonSerDer
import io.github.crabzilla.engine.command.CommandController
import io.github.crabzilla.engine.command.CommandsContext
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.stack.StateId
import io.github.crabzilla.stack.command.CommandMetadata
import io.kotest.matchers.throwable.shouldHaveMessage
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandsValidationIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var commandsContext: CommandsContext
  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repository: SnapshotTestRepository<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
    commandController = CommandController(vertx, customerConfig, commandsContext.pgPool, jsonSerDer, true)
    repository = SnapshotTestRepository(commandsContext.pgPool, example1Json)
    testRepo = TestRepository(commandsContext.pgPool)
    cleanDatabase(commandsContext.sqlClient)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can validate command")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "bad customer")
    val metadata = CommandMetadata(StateId(id))
    commandController.handle(metadata, cmd)
      .onFailure {
        it shouldHaveMessage "[Bad customer!]"
        tc.completeNow()
      }
      .onSuccess {
        tc.failNow("It should fail")
      }
  }

  @Test
  @DisplayName("it can validate command within command handler")
  fun s2(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata(StateId(id))
    commandController.handle(metadata, cmd)
      .compose {
        commandController.handle(
          CommandMetadata(StateId(id)),
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
