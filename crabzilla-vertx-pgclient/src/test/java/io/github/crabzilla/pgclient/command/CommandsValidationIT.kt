package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.TestRepository
import io.github.crabzilla.pgclient.command.internal.PersistentSnapshotRepo
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
@DisplayName("Validating commands")
class CommandsValidationIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    val pgPool = pgPool(vertx)
    val snapshotRepo2 = PersistentSnapshotRepo<Customer, CustomerEvent>(customerConfig.name, jsonSerDer)
    commandController = CommandController(vertx, pgPool, jsonSerDer, customerConfig, snapshotRepo2)
    testRepo = TestRepository(pgPool)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can validate before command handler`(vertx: Vertx, tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "bad customer")
    val metadata = CommandMetadata(id)
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
  fun `it can validate within command handler`(vertx: Vertx, tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata(id)
    commandController.handle(metadata, cmd)
      .compose {
        commandController.handle(
          CommandMetadata(id),
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
