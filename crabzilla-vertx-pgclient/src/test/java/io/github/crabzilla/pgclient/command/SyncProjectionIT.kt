package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomersEventsProjector
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.TestRepository
import io.github.crabzilla.pgclient.command.internal.OnDemandSnapshotRepo
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
@DisplayName("Running synchronous projection")
class SyncProjectionIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var pgPool: PgPool
  private lateinit var controller: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var snapshotRepository: SnapshotRepository<Customer, CustomerEvent>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    pgPool = pgPool(vertx)
    controller = CommandController.create(
      vertx, pgPool, jsonSerDer,
      customerConfig, SnapshotType.PERSISTENT,
      CustomersEventsProjector("customers")
    )
    snapshotRepository = OnDemandSnapshotRepo(customerEventHandler, jsonSerDer)
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
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.getAllCustomers()
          .onFailure { tc.failNow(it) }
          .onSuccess { customersList ->
            assertThat(customersList.size).isEqualTo(1)
            val json = customersList.first()
//            println(json.encodePrettily())
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
    val metadata1 = CommandMetadata(id)

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(id)

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
    pgPool.connection.compose { conn ->
      snapshotRepository.get(conn, id)
        .onFailure { tc.failNow(it) }
        .onSuccess { snapshot0 ->
          assert(snapshot0 == null)
          controller.handle(CommandMetadata(id), CustomerCommand.RegisterCustomer(id, "cust#$id"))
            .onFailure { tc.failNow(it) }
            .onSuccess {
              snapshotRepository.get(conn, id)
                .onFailure { err -> tc.failNow(err) }
                .onSuccess { snapshot1 ->
                  assert(1 == snapshot1!!.version)
                  assert(snapshot1.state == Customer(id, "cust#$id"))
                  controller.handle(CommandMetadata(id), CustomerCommand.ActivateCustomer("because yes"))
                    .onFailure { tc.failNow(it) }
                    .onSuccess {
                      snapshotRepository.get(conn, id)
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
}
