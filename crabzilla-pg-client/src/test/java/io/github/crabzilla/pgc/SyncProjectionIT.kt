package io.github.crabzilla.pgc

import io.github.crabzilla.core.command.StatefulSession
import io.github.crabzilla.core.serder.KotlinSerDer
import io.github.crabzilla.core.serder.SerDer
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.pgc.command.CommandController
import io.github.crabzilla.pgc.command.CommandsContext
import io.github.crabzilla.stack.StateId
import io.github.crabzilla.stack.command.CommandMetadata
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
class SyncProjectionIT {

  private lateinit var serDer: SerDer
  private lateinit var client: CommandsContext
  private lateinit var controller: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var snapshotRepository: SnapshotRepository<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    serDer = KotlinSerDer(example1Json)
    client = CommandsContext.create(vertx, serDer, connectOptions, poolOptions)
    controller = client.create(
      customerConfig,
      saveCommandOption = true,
      advisoryLockOption = true,
      eventsProjector = CustomerEventsProjector
    )
    snapshotRepository = SnapshotRepository(client.pgPool, example1Json)
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
    val metadata = CommandMetadata(StateId(id))
    val constructorResult = Customer.create(id, cmd.name)
    val session = StatefulSession(constructorResult, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    controller.handle(metadata, cmd)
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
    val metadata1 = CommandMetadata(StateId(id))

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(StateId(id))

    controller.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(metadata2, cmd2)
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

  @Test
  @DisplayName("checking the snapshot")
  fun a0(tc: VertxTestContext, vertx: Vertx) {
    val id = UUID.randomUUID()
    snapshotRepository.get(id)
      .onFailure { tc.failNow(it) }
      .onSuccess { snapshot0 ->
        assert(snapshot0 == null)
        controller.handle(CommandMetadata(StateId(id)), CustomerCommand.RegisterCustomer(id, "cust#$id"))
          .onFailure { tc.failNow(it) }
          .onSuccess {
            snapshotRepository.get(id)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { snapshot1 ->
                assert(1 == snapshot1!!.version)
                assert(snapshot1.state == Customer(id, "cust#$id"))
                controller.handle(CommandMetadata(StateId(id)), CustomerCommand.ActivateCustomer("because yes"))
                  .onFailure { tc.failNow(it) }
                  .onSuccess {
                    snapshotRepository.get(id)
                      .onFailure { err -> tc.failNow(err) }
                      .onSuccess { snapshot2 ->
                        assert(2 == snapshot2!!.version)
                        val expected = Customer(id, "cust#$id", isActive = true, reason = "because yes")
                        assert(snapshot2.state == expected)
                        tc.completeNow()
                      }
                  }
              }
          }
      }
  }
}
