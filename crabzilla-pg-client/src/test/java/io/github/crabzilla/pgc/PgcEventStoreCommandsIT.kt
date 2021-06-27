package io.github.crabzilla.pgc

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerEventHandler
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.stack.AggregateRootId
import io.github.crabzilla.stack.CommandMetadata
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
class PgcEventStoreCommandsIT {

  private lateinit var pgPool: PgPool
  private lateinit var eventStore: PgcEventStore<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repo: PgcSnapshotRepo<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: PgcTestRepoHelper

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        pgPool = getPgPool(vertx, config)
        eventStore = PgcEventStore(customerConfig, pgPool, true)
        repo = PgcSnapshotRepo(customerConfig, pgPool)
        testRepo = PgcTestRepoHelper(pgPool)
        cleanDatabase(vertx, config)
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess {
        tc.completeNow()
      }
  }

  @Test
  @DisplayName("appending 1 command with 2 events results in version 2 ")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata(AggregateRootId(id))
    val constructorResult = Customer.create(id, cmd.name)
    val session = StatefulSession(constructorResult, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    eventStore.append(cmd, metadata, session)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.getAllCommands()
          .onFailure { tc.failNow(it) }
          .onSuccess { list ->
            assertThat(list.size).isEqualTo(1)
            val rowAsJson = list.first()
            assertThat(UUID.fromString(rowAsJson.getString("cmd_id"))).isEqualTo(metadata.commandId.id)
            val cmdAsJsonFroDb = rowAsJson.getJsonObject("cmd_payload")
            val cmdFromDb = Command.fromJson<RegisterAndActivateCustomer>(customerJson, cmdAsJsonFroDb.toString())
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
    val metadata1 = CommandMetadata(AggregateRootId(id))
    val constructorResult = Customer.create(id, cmd1.name)
    val session1 = StatefulSession(constructorResult, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = DeactivateCustomer("it's not needed anymore")
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
            testRepo.getAllCommands()
              .onFailure { tc.failNow(it) }
              .onSuccess { list ->
                assertThat(list.size).isEqualTo(2)

                val rowAsJson1 = list.first()
                assertThat(UUID.fromString(rowAsJson1.getString("cmd_id"))).isEqualTo(metadata1.commandId.id)
                val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("cmd_payload")
                val cmdFromDb1 = Command.fromJson<RegisterAndActivateCustomer>(customerJson, cmdAsJsonFroDb1.toString())
                assertThat(cmdFromDb1).isEqualTo(cmd1)

                val rowAsJson2 = list.last()
                assertThat(UUID.fromString(rowAsJson2.getString("cmd_id"))).isEqualTo(metadata2.commandId.id)
                val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("cmd_payload")
                val cmdFromDb2 = Command.fromJson<DeactivateCustomer>(customerJson, cmdAsJsonFroDb2.toString())
                assertThat(cmdFromDb2).isEqualTo(cmd2)

                tc.completeNow()
              }
          }
      }
  }
}
