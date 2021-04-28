package io.github.crabzilla.pgc

import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.example1.customerEventHandler
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
class PgcEventStoreIT {

  // TODO check if appended data match

  private lateinit var writeDb: PgPool
  private lateinit var eventStore: PgcEventStore<Customer, CustomerCommand, CustomerEvent>

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        eventStore = PgcEventStore(customerConfig, writeDb)
        cleanDatabase(vertx, config)
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess {
        tc.completeNow()
      }
  }

  @Test
  @DisplayName("can append version 1")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val customer = Customer.create(id = id, name = "c1") // AggregateRootSession needs a non null state
    val cmd = CustomerCommand.ActivateCustomer("is needed")
    val metadata = CommandMetadata(AggregateRootId(id))
    val session = StatefulSession(0, customer.state, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    eventStore.append(cmd, metadata, session)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("can append version 1 then version 2")
  fun s11(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val customer = Customer.create(id = id, name = "c1")
    val cmd1 = CustomerCommand.ActivateCustomer("is needed")
    val metadata1 = CommandMetadata(AggregateRootId(id))
    val session1 = StatefulSession(0, customer.state, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(AggregateRootId(id))
    val session2 = StatefulSession(1, customer.state, customerEventHandler)
    session2.execute { it.deactivate(cmd1.reason) }

    eventStore.append(cmd1, metadata1, session1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.append(cmd2, metadata2, session2)
          .onFailure { tc.failNow(it) }
          .onSuccess { tc.completeNow() }
      }
  }

  @Test
  @DisplayName("cannot append version 1 twice")
  fun s2(tc: VertxTestContext) {
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
            tc.verify { assertThat(err.message).isEqualTo("The current version is already the expected new version 1") }
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("cannot append version 3 after version 1")
  fun s22(tc: VertxTestContext) {
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
