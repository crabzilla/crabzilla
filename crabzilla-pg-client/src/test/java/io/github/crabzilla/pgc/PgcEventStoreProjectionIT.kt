package io.github.crabzilla.pgc

import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.CustomerEventsProjector
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerEventHandler
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.pgc.command.CommandControllerClient
import io.github.crabzilla.pgc.command.PgcEventStore
import io.github.crabzilla.pgc.command.PgcSnapshotRepo
import io.github.crabzilla.stack.AggregateRootId
import io.github.crabzilla.stack.CommandMetadata
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
class PgcEventStoreProjectionIT {

  private lateinit var client: CommandControllerClient
  private lateinit var eventStore: PgcEventStore<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repo: PgcSnapshotRepo<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    client = CommandControllerClient.create(vertx, customerJson, connectOptions, poolOptions)
    eventStore = PgcEventStore(
      customerConfig, client.pgPool, client.json,
      saveCommandOption = true,
      optimisticLockOption = true,
      eventsProjectorApi = CustomerEventsProjector
    )
    repo = PgcSnapshotRepo(client.pgPool, client.json)
    testRepo = TestRepository(client.pgPool)
    cleanDatabase(client.sqlClient)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("appending 1 command with 2 events results in version 2 ")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata(AggregateRootId(id))
    val constructorResult = Customer.create(id, cmd.name)
    val session = StatefulSession(constructorResult, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    eventStore.append(cmd, metadata, session)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.getAllCustomers()
          .onFailure { tc.failNow(it) }
          .onSuccess { customersList ->
            assertThat(customersList.size).isEqualTo(1)
            val json = customersList.first()
            assertThat(UUID.fromString(json.getString("id"))).isEqualTo(id)
            assertThat(json.getString("name")).isEqualTo(cmd.name)
            assertThat(json.getBoolean("is_active")).isEqualTo(true)
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("appending 2 commands with 2 and 1 event, respectively results in version 3")
  fun s2(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd1 = CustomerCommand.RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata(AggregateRootId(id))
    val constructorResult = Customer.create(id, cmd1.name)
    val session1 = StatefulSession(constructorResult, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(AggregateRootId(id))
    val customer2 = Customer(id, cmd1.name, true, cmd2.reason)
    val session2 = StatefulSession(2, customer2, customerEventHandler)
    session2.execute { it.deactivate(cmd2.reason) }

    eventStore.append(cmd1, metadata1, session1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.append(cmd2, metadata2, session2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            testRepo.getAllCustomers()
              .onFailure { tc.failNow(it) }
              .onSuccess { customersList ->
                assertThat(customersList.size).isEqualTo(1)
                val json = customersList.first()
                assertThat(UUID.fromString(json.getString("id"))).isEqualTo(id)
                assertThat(json.getString("name")).isEqualTo(cmd1.name)
                assertThat(json.getBoolean("is_active")).isEqualTo(false)
                tc.completeNow()
              }
          }
      }
  }
}
