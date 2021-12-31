package io.github.crabzilla.command

import io.github.crabzilla.command.internal.PersistentSnapshotRepo
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
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
class CommandsNotificationIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var commandsContext: CommandsContext
  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repository: SnapshotTestRepository<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    commandsContext = CommandsContext.create(vertx, jsonSerDer, config)
    val snapshotRepo2 = PersistentSnapshotRepo<Customer, CustomerEvent>(customerConfig.name, jsonSerDer)
    commandController = CommandController(
      vertx = vertx, pgPool = commandsContext.pgPool,
      jsonSerDer = jsonSerDer, config = customerConfig, snapshotRepository = snapshotRepo2, notificationsInterval = 5
    )
    repository = SnapshotTestRepository(commandsContext.pgPool, example1Json)
    testRepo = TestRepository(commandsContext.pgPool)
    cleanDatabase(commandsContext.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it should notify each 5 ms")
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
