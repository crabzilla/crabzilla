package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.TestRepository
import io.github.crabzilla.pgclient.command.internal.PersistentSnapshotRepo
import io.github.crabzilla.pgclient.command.internal.SnapshotRepository
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Storing persistent snapshots")
class PersistentSnapshotIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var pgPool: PgPool
  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var snapshotRepository: SnapshotRepository<Customer, CustomerEvent>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    pgPool = pgPool(vertx)
    snapshotRepository = PersistentSnapshotRepo(customerConfig.name, jsonSerDer)
    commandController = CommandController(vertx, pgPool, jsonSerDer, customerConfig, snapshotRepository)
    testRepo = TestRepository(pgPool)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("appending 1 command with 2 events results in version 2 ")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata(id)
    val constructorResult = Customer.create(id, cmd.name)
    val session = CommandSession(constructorResult, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    commandController.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        snapshotRepository.get(pgPool.connection.result(), id)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            assertThat(it!!.version).isEqualTo(2)
            assertThat(it.state).isEqualTo(Customer(id, cmd.name, true, cmd.reason))
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("appending 2 commands with 2 and 1 event, respectively results in version 3")
  fun s11(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd1 = CustomerCommand.RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata(id)
    val constructorResult = Customer.create(id, cmd1.name)
    val session1 = CommandSession(constructorResult, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(id)
    val customer2 = Customer(id, cmd1.name, true, cmd2.reason)
    val session2 = CommandSession(customer2, customerEventHandler)
    session2.execute { it.deactivate(cmd2.reason) }

    commandController.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        commandController.handle(metadata2, cmd2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            snapshotRepository.get(pgPool.connection.result(), id)
              .onFailure { tc.failNow(it) }
              .onSuccess {
                assertThat(it!!.version).isEqualTo(3)
                val expectedCustomer = customer2.copy(isActive = false, reason = cmd2.reason)
                assertThat(it.state).isEqualTo(expectedCustomer)
                tc.completeNow()
              }
          }
      }
  }
}
