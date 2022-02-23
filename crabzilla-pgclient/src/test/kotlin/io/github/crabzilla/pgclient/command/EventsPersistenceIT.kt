package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.example1.customer.example1Json
import io.github.crabzilla.pgclient.TestRepository
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
@DisplayName("Persisting events")
class EventsPersistenceIT {

  private lateinit var pgPool: PgPool
  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    pgPool = pgPool(vertx)
    commandController = CommandControllerBuilder(vertx, pgPool)
      .build(example1Json, customerConfig, SnapshotType.PERSISTENT)
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
    val metadata = CommandMetadata.new(id)
    val constructorResult = Customer.create(id, cmd.name)
    val session = CommandSession(constructorResult, customerEventHandler)
    session.execute { it.activate(cmd.reason) }
    commandController.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.scanEvents(0, 10)
          .onFailure { it2 -> tc.failNow(it2) }
          .onSuccess { eventsList ->
            assertThat(eventsList.size).isEqualTo(2)
            // check register event
            val asJson1 = eventsList[0]
//                println(asJson1.encodePrettily())
            assertThat(asJson1.getString("state_type")).isEqualTo(customerConfig.stateSerialName())
            assertThat(asJson1.getString("state_id")).isEqualTo(id.toString())
            assertThat(asJson1.getInteger("version")).isEqualTo(1)
            val expectedEvent1 = CustomerEvent.CustomerRegistered(id, cmd.name)
            val json1 = asJson1.getJsonObject("event_payload").toString()
            val event1 = example1Json.decodeFromString(customerConfig.eventSerDer, json1)
            assertThat(expectedEvent1).isEqualTo(event1)
            assertThat(asJson1.getString("causation_id")).isEqualTo(metadata.commandId.toString())
            assertThat(asJson1.getString("correlation_id")).isEqualTo(metadata.commandId.toString())
            // check activate event
            val asJson = eventsList[1]
//                println(asJson.encodePrettily())
            assertThat(asJson.getString("state_type")).isEqualTo(customerConfig.stateSerialName())
            assertThat(asJson.getString("state_id")).isEqualTo(id.toString())
            assertThat(asJson.getInteger("version")).isEqualTo(2)
            val expectedEvent = CustomerEvent.CustomerActivated(cmd.reason)
            val json = asJson.getJsonObject("event_payload").toString()
            val event = example1Json.decodeFromString(customerConfig.eventSerDer, json)
            assertThat(expectedEvent).isEqualTo(event)
            val causationId = asJson1.getString("id")
            assertThat(asJson.getString("causation_id")).isEqualTo(causationId)
            assertThat(asJson.getString("correlation_id")).isEqualTo(metadata.commandId.toString())
            tc.completeNow()
          }
      }
  }

  @Test
  @DisplayName("appending 2 commands with 2 and 1 event, respectively results in version 3")
  fun s11(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd1 = CustomerCommand.RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata.new(id)
    val constructorResult = Customer.create(id, cmd1.name)
    val session1 = CommandSession(constructorResult, customerEventHandler)
    session1.execute { it.activate(cmd1.reason) }

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata.new(id)
    val customer2 = Customer(id, cmd1.name, true, cmd2.reason)
    val session2 = CommandSession(customer2, customerEventHandler)
    session2.execute { it.deactivate(cmd2.reason) }

    commandController.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        commandController.handle(metadata2, cmd2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            testRepo.scanEvents(0, 10)
              .onFailure { it2 -> tc.failNow(it2) }
              .onSuccess { eventsList ->
                assertThat(eventsList.size).isEqualTo(3)
                // check register event
                val asJson1 = eventsList[0]
                // println(asJson1.encodePrettily())
                assertThat(asJson1.getString("state_type")).isEqualTo(customerConfig.stateSerialName())
                assertThat(asJson1.getString("state_id")).isEqualTo(id.toString())
                assertThat(asJson1.getInteger("version")).isEqualTo(1)
                val expectedEvent1 = CustomerEvent.CustomerRegistered(id, cmd1.name)
                val json1 = asJson1.getString("event_payload")
                val event1 = example1Json.decodeFromString(customerConfig.eventSerDer, json1)
                assertThat(expectedEvent1).isEqualTo(event1)
                assertThat(asJson1.getString("causation_id")).isEqualTo(metadata1.commandId.toString())
                assertThat(asJson1.getString("correlation_id")).isEqualTo(metadata1.commandId.toString())
                // check activate event
                val asJson2 = eventsList[1]
                // println(asJson2.encodePrettily())
                assertThat(asJson2.getString("state_type")).isEqualTo(customerConfig.stateSerialName())
                assertThat(asJson2.getString("state_id")).isEqualTo(id.toString())
                assertThat(asJson2.getInteger("version")).isEqualTo(2)
                val expectedEvent2 = CustomerEvent.CustomerActivated(cmd1.reason)
                val json2 = asJson2.getString("event_payload")
                val event2 = example1Json.decodeFromString(customerConfig.eventSerDer, json2)
                assertThat(expectedEvent2).isEqualTo(event2)
                val causationId2 = asJson1.getString("id")
                assertThat(asJson2.getString("causation_id")).isEqualTo(causationId2)
                assertThat(asJson2.getString("correlation_id")).isEqualTo(metadata1.commandId.toString())
                // check deactivate events
                val asJson3 = eventsList[2]
                // println(asJson3.encodePrettily())
                assertThat(asJson3.getString("state_type")).isEqualTo(customerConfig.stateSerialName())
                assertThat(asJson3.getString("state_id")).isEqualTo(id.toString())
                assertThat(asJson3.getInteger("version")).isEqualTo(3)
                val expectedEvent3 = CustomerEvent.CustomerDeactivated(cmd2.reason)
                val json3 = asJson3.getString("event_payload")
                val event3 = example1Json.decodeFromString(customerConfig.eventSerDer, json3)
                assertThat(expectedEvent3).isEqualTo(event3)
                assertThat(asJson3.getString("causation_id")).isEqualTo(metadata2.commandId.toString())
                assertThat(asJson3.getString("correlation_id")).isEqualTo(metadata2.commandId.toString())
                tc.completeNow()
              }
          }
      }
  }
}
