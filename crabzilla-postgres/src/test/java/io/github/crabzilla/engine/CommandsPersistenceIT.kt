package io.github.crabzilla.engine

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.json.KotlinJsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.github.crabzilla.engine.command.CommandController
import io.github.crabzilla.engine.command.CommandsContext
import io.github.crabzilla.engine.command.OnDemandSnapshotRepo
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandsPersistenceIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var commandsContext: CommandsContext
  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repository: SnapshotTestRepository<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
    val snapshotRepo2 = OnDemandSnapshotRepo(customerConfig.eventHandler, jsonSerDer)
//    val snapshotRepo2 = PersistentSnapshotRepo<Customer, CustomerEvent>(customerConfig.name, jsonSerDer)
    commandController = CommandController(vertx, commandsContext.pgPool, jsonSerDer, customerConfig, snapshotRepo2)
    repository = SnapshotTestRepository(commandsContext.pgPool, example1Json)
    testRepo = TestRepository(commandsContext.pgPool)
    cleanDatabase(commandsContext.sqlClient)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("appending 1 command with 2 events results in version 2 ")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata(StateId(id))
    commandController.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.getAllCommands()
          .onFailure { tc.failNow(it) }
          .onSuccess { list ->
            assertThat(list.size).isEqualTo(1)
            val rowAsJson = list.first()
            assertThat(UUID.fromString(rowAsJson.getString("cmd_id"))).isEqualTo(metadata.commandId.id)
            val cmdAsJsonFroDb = rowAsJson.getJsonObject("cmd_payload")
            val cmdFromDb = jsonSerDer.commandFromJson(cmdAsJsonFroDb.toString()) as RegisterAndActivateCustomer
            assertThat(cmdFromDb).isEqualTo(cmd)
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("appending 2 commands with 2 and 1 event, respectively results in version 3")
  fun s2(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata(StateId(id))

    val cmd2 = DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(StateId(id))

    commandController.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        commandController.handle(metadata2, cmd2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            testRepo.getAllCommands()
              .onFailure { tc.failNow(it) }
              .onSuccess { list ->
                assertThat(list.size).isEqualTo(2)

                val rowAsJson1 = list.first()
                assertThat(UUID.fromString(rowAsJson1.getString("cmd_id"))).isEqualTo(metadata1.commandId.id)
                val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("cmd_payload")
                val cmdFromDb1 = jsonSerDer.commandFromJson(cmdAsJsonFroDb1.toString())
                assertThat(cmdFromDb1).isEqualTo(cmd1)

                val rowAsJson2 = list.last()
                assertThat(UUID.fromString(rowAsJson2.getString("cmd_id"))).isEqualTo(metadata2.commandId.id)
                val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("cmd_payload")
                val cmdFromDb2 = jsonSerDer.commandFromJson(cmdAsJsonFroDb2.toString())
                assertThat(cmdFromDb2).isEqualTo(cmd2)

                tc.completeNow()
              }
          }
      }
  }
}
