package io.github.crabzilla.pgc

import io.github.crabzilla.core.command.StatefulSession
import io.github.crabzilla.core.serder.KotlinSerDer
import io.github.crabzilla.core.serder.SerDer
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
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
class CorrelationCausationIdsIT {

  private lateinit var serDer: SerDer
  private lateinit var client: CommandsContext
  private lateinit var eventStore: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repository: SnapshotRepository<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    serDer = KotlinSerDer(example1Json)
    client = CommandsContext.create(vertx, serDer, connectOptions, poolOptions)
    eventStore = CommandController(vertx, customerConfig, client.pgPool, serDer, false)
    repository = SnapshotRepository(client.pgPool, example1Json)
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
    eventStore.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        repository.get(id)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            assertThat(it!!.version).isEqualTo(2)
            assertThat(it.state).isEqualTo(Customer(id, cmd.name, true, cmd.reason))
            testRepo.scanEvents(0, 10)
              .onFailure { it2 -> tc.failNow(it2) }
              .onSuccess { eventsList ->
                assertThat(eventsList.size).isEqualTo(2)
                // check register event
                val asJson1 = eventsList[0]
//                println(asJson1.encodePrettily())
                assertThat(asJson1.getString("ar_name")).isEqualTo(customerConfig.name)
                assertThat(asJson1.getString("ar_id")).isEqualTo(id.toString())
                assertThat(asJson1.getInteger("version")).isEqualTo(1)
                val expectedEvent1 = CustomerEvent.CustomerRegistered(id, cmd.name)
                val json1 = asJson1.getString("event_payload")
                val event1 = serDer.eventFromJson(json1)
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
                val event = serDer.eventFromJson(json)
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
  @DisplayName("appending 2 commands with 2 and 1 event, respectively results in version 3")
  fun s11(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd1 = CustomerCommand.RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata(StateId(id))
    val constructorResult = Customer.create(id, cmd1.name)
    val session1 = StatefulSession(constructorResult, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata(StateId(id))
    val customer2 = Customer(id, cmd1.name, true, cmd2.reason)
    val session2 = StatefulSession(2, customer2, customerEventHandler)
    session2.execute { it.deactivate(cmd2.reason) }

    eventStore.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        eventStore.handle(metadata2, cmd2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            repository.get(id)
              .onFailure { tc.failNow(it) }
              .onSuccess {
                assertThat(it!!.version).isEqualTo(3)
                val expectedCustomer = customer2.copy(isActive = false, reason = cmd2.reason)
                assertThat(it.state).isEqualTo(expectedCustomer)
                testRepo.scanEvents(0, 10)
                  .onFailure { it2 -> tc.failNow(it2) }
                  .onSuccess { eventsList ->
                    assertThat(eventsList.size).isEqualTo(3)
                    // check register event
                    val asJson1 = eventsList[0]
                    // println(asJson1.encodePrettily())
                    assertThat(asJson1.getString("ar_name")).isEqualTo(customerConfig.name)
                    assertThat(asJson1.getString("ar_id")).isEqualTo(id.toString())
                    assertThat(asJson1.getInteger("version")).isEqualTo(1)
                    val expectedEvent1 = CustomerEvent.CustomerRegistered(id, cmd1.name)
                    val json1 = asJson1.getString("event_payload")
                    val event1 = serDer.eventFromJson(json1)
                    assertThat(expectedEvent1).isEqualTo(event1)
                    assertThat(asJson1.getString("causation_id")).isEqualTo(metadata1.commandId.id.toString())
                    assertThat(asJson1.getString("correlation_id")).isEqualTo(metadata1.commandId.id.toString())
                    // check activate event
                    val asJson2 = eventsList[1]
                    // println(asJson2.encodePrettily())
                    assertThat(asJson2.getString("ar_name")).isEqualTo(customerConfig.name)
                    assertThat(asJson2.getString("ar_id")).isEqualTo(id.toString())
                    assertThat(asJson2.getInteger("version")).isEqualTo(2)
                    val expectedEvent2 = CustomerEvent.CustomerActivated(cmd1.reason)
                    val json2 = asJson2.getString("event_payload")
                    val event2 = serDer.eventFromJson(json2)
                    assertThat(expectedEvent2).isEqualTo(event2)
                    val causationId2 = asJson1.getString("id")
                    assertThat(asJson2.getString("causation_id")).isEqualTo(causationId2)
                    assertThat(asJson2.getString("correlation_id")).isEqualTo(metadata1.commandId.id.toString())
                    // check deactivate events
                    val asJson3 = eventsList[2]
                    // println(asJson3.encodePrettily())
                    assertThat(asJson3.getString("ar_name")).isEqualTo(customerConfig.name)
                    assertThat(asJson3.getString("ar_id")).isEqualTo(id.toString())
                    assertThat(asJson3.getInteger("version")).isEqualTo(3)
                    val expectedEvent3 = CustomerEvent.CustomerDeactivated(cmd2.reason)
                    val json3 = asJson3.getString("event_payload")
                    val event3 = serDer.eventFromJson(json3)
                    assertThat(expectedEvent3).isEqualTo(event3)
                    assertThat(asJson3.getString("causation_id")).isEqualTo(metadata2.commandId.id.toString())
                    assertThat(asJson3.getString("correlation_id")).isEqualTo(metadata2.commandId.id.toString())
                    tc.completeNow()
                  }
              }
          }
      }
  }
}
