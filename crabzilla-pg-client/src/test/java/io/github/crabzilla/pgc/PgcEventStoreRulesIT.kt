package io.github.crabzilla.pgc

import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
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
class PgcEventStoreRulesIT {

  private lateinit var pgPool: PgPool
  private lateinit var eventStore: PgcEventStore<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repo: PgcSnapshotRepo<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: PgcTestRepoHelper

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        pgPool = getPgPool(vertx, config)
        eventStore = PgcEventStore(customerConfig, pgPool, customerJson, false)
        repo = PgcSnapshotRepo(customerConfig, pgPool, customerJson)
        testRepo = PgcTestRepoHelper(pgPool)
        cleanDatabase(vertx, config)
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess {
        tc.completeNow()
      }
  }

  @Test
  @DisplayName("cannot append version 1 twice")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val customer = Customer.create(id = id, name = "c1")
    val cmd1 = CustomerCommand.ActivateCustomer("is needed")
    val metadata1 = CommandMetadata(AggregateRootId(id))
    val session1 = StatefulSession(0, customer.state, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(AggregateRootId(id))
    val session2 = StatefulSession(0, customer.state, customerEventHandler)
    session2.execute { it.deactivate(cmd1.reason) }

    eventStore.append(cmd1, metadata1, session1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.append(cmd2, metadata2, session2)
          .onSuccess { tc.failNow("should fail") }
          .onFailure { err ->
            tc.verify { assertThat(err.message).isEqualTo("The current version [1] should be [0]") }
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("cannot append version 3 after version 1")
  fun s2(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val customer = Customer.create(id = id, name = "c1")

    val cmd1 = CustomerCommand.ActivateCustomer("is needed")
    val metadata1 = CommandMetadata(AggregateRootId(id))
    val session1 = StatefulSession(0, customer.state, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(AggregateRootId(id))
    val session2 = StatefulSession(2, customer.state, customerEventHandler)
    session2.execute { it.deactivate(cmd1.reason) }

    eventStore.append(cmd1, metadata1, session1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.append(cmd2, metadata2, session2)
          .onSuccess { tc.failNow("should fail") }
          .onFailure { err ->
            tc.verify { assertThat(err.message).isEqualTo("The current version [1] should be [2]") }
            tc.completeNow()
          }
      }
  }
}
