package io.github.crabzilla.pgc

import io.github.crabzilla.core.DomainEvent
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
class PgcEventStoreIT {

  // TODO check if appended data match

  private lateinit var writeDb: PgPool
  private lateinit var eventStore: PgcEventStore<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repo: PgcSnapshotRepo<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: PgcEventRepoTestHelper

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    getConfig(vertx)
      .compose { config ->
        writeDb = writeModelPgPool(vertx, config)
        eventStore = PgcEventStore(customerConfig, writeDb)
        repo = PgcSnapshotRepo(customerConfig, writeDb)
        testRepo = PgcEventRepoTestHelper(writeDb)
        cleanDatabase(vertx, config)
      }
      .onFailure { tc.failNow(it.cause) }
      .onSuccess {
        tc.completeNow()
      }
  }

  @Test
  @DisplayName("can append version 1 with 2 events")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val customer = Customer.create(id = id, name = "c1") // AggregateRootSession needs a non null state
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, customer.state.name, "is needed")
    val metadata = CommandMetadata(AggregateRootId(id))
    val constructorResult = Customer.create(id = id, name = customer.state.name)
    val session = StatefulSession(constructorResult, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    eventStore.append(cmd, metadata, session)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        repo.get(id)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            assertThat(it!!.version).isEqualTo(2)
            testRepo.scanEvents(0, 10)
              .onFailure { it2 -> tc.failNow(it2) }
              .onSuccess { eventsList ->
                assertThat(eventsList.size).isEqualTo(2)
                // check create event
                val asJson1 = eventsList[0]
                println(asJson1.encodePrettily())
                assertThat(asJson1.getString("ar_name")).isEqualTo(customerConfig.name)
                assertThat(asJson1.getString("ar_id")).isEqualTo(id.toString())
                assertThat(asJson1.getInteger("version")).isEqualTo(1)
                val expectedEvent1 = CustomerEvent.CustomerRegistered(customer.state.id, customer.state.name)
                val json1 = asJson1.getString("event_payload")
                val event1 = DomainEvent.fromJson<CustomerEvent.CustomerRegistered>(customerJson, json1)
                assertThat(expectedEvent1).isEqualTo(event1)
                assertThat(asJson1.getString("causation_id")).isEqualTo(metadata.commandId.id.toString())
                assertThat(asJson1.getString("correlation_id")).isEqualTo(metadata.commandId.id.toString())
                // check activate event
                val asJson = eventsList[1]
//                println(asJson.encodePrettily())
                assertThat(asJson.getString("ar_name")).isEqualTo(customerConfig.name)
                assertThat(asJson.getString("ar_id")).isEqualTo(id.toString())
                assertThat(asJson.getInteger("version")).isEqualTo(2)
                val expectedEvent = CustomerEvent.CustomerActivated(cmd.reason)
                val json = asJson.getString("event_payload")
                val event = DomainEvent.fromJson<CustomerEvent.CustomerActivated>(customerJson, json)
                assertThat(expectedEvent).isEqualTo(event)
                val causationId = asJson1.getString("id")
                assertThat(asJson.getString("causation_id")).isEqualTo(causationId)
                assertThat(asJson.getString("correlation_id")).isEqualTo(metadata.commandId.id.toString())
                tc.completeNow()
              }
          }
      }
  }

  @Test
  @DisplayName("can append version 2 (session with 2 events)")
  fun s12(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val customer = Customer.create(id = id, name = "c1") // AggregateRootSession needs a non null state
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, customer.state.name, "is needed")
    val metadata = CommandMetadata(AggregateRootId(id))
    val constructorResult = Customer.create(id = id, name = customer.state.name)
    val session = StatefulSession(constructorResult, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    eventStore.append(cmd, metadata, session)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        repo.get(id)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            assertThat(it!!.version).isEqualTo(2)
            tc.completeNow()
          }
      }
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
          .onSuccess {
            repo.get(id)
              .onFailure { tc.failNow(it) }
              .onSuccess {
                assertThat(it!!.version).isEqualTo(2)
                tc.completeNow()
              }
          }
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
            tc.verify { assertThat(err.message).isEqualTo("The current version [1] should be [0]") }
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
