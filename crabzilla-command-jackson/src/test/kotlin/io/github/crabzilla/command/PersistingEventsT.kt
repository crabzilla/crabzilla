package io.github.crabzilla.command

import io.github.crabzilla.Jackson
import io.github.crabzilla.Jackson.json
import io.github.crabzilla.TestRepository.Companion.testRepo
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.pgPool
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.CommandSideEffect
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
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
@DisplayName("Persisting events")
class PersistingEventsT {

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("appending 1 command with 2 events results in version 2 ")
  fun s1(tc: VertxTestContext, vertx: Vertx) {

    val repository = JacksonCommandRepository(json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, repository)

    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata.new(id)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.scanEvents(0, 10)
          .onFailure { it2 -> tc.failNow(it2) }
          .onSuccess { eventsList ->
            assertThat(eventsList.size).isEqualTo(2)
            // check register event
            val asJson1 = eventsList[0]
            assertThat(asJson1.getString("state_type")).isEqualTo("Customer")
            assertThat(asJson1.getString("state_id")).isEqualTo(id.toString())
            assertThat(asJson1.getInteger("version")).isEqualTo(1)
            val expectedEvent1 = CustomerEvent.CustomerRegistered(id, cmd.name)
            val json1 = asJson1.getJsonObject("event_payload").toString()
            val event1 = json.readValue(json1, CustomerEvent::class.java)
            assertThat(expectedEvent1).isEqualTo(event1)
            assertThat(asJson1.getString("causation_id")).isEqualTo(metadata.commandId.toString())
            assertThat(asJson1.getString("correlation_id")).isEqualTo(metadata.commandId.toString())
            // check activate event
            val asJson = eventsList[1]
            assertThat(asJson.getString("state_type")).isEqualTo("Customer")
            assertThat(asJson.getString("state_id")).isEqualTo(id.toString())
            assertThat(asJson.getInteger("version")).isEqualTo(2)
            val expectedEvent = CustomerEvent.CustomerActivated(cmd.reason)
            val json = asJson.getJsonObject("event_payload").toString()
            val event = Jackson.json.readValue(json, CustomerEvent::class.java)
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
  fun s11(tc: VertxTestContext, vertx: Vertx) {

    val repository = JacksonCommandRepository(json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, repository)

    val id = UUID.randomUUID()
    val cmd1 = CustomerCommand.RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata.new(id)
    controller.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess { sideEffect1: CommandSideEffect ->
        val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
        val metadata2 = CommandMetadata.new(id, sideEffect1.correlationId(), sideEffect1.latestEventId())
        controller.handle(metadata2, cmd2)
          .onFailure { err -> tc.failNow(err) }
          .onSuccess {
            testRepo.scanEvents(0, 10)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { eventsList: List<JsonObject> ->
                eventsList.forEachIndexed { index, it ->
                  println("$index -> ${it.encodePrettily()}")
                }
                assertThat(eventsList.size).isEqualTo(3)
                // check register event
                val asJson1 = eventsList[0]
                // println(asJson1.encodePrettily())
                assertThat(asJson1.getString("state_type")).isEqualTo("Customer")
                assertThat(asJson1.getString("state_id")).isEqualTo(id.toString())
                assertThat(asJson1.getInteger("version")).isEqualTo(1)
                val expectedEvent1 = CustomerEvent.CustomerRegistered(id, cmd1.name)
                val json1 = asJson1.getString("event_payload")
                val event1 = json.readValue(json1, CustomerEvent::class.java)
                assertThat(expectedEvent1).isEqualTo(event1)
                assertThat(asJson1.getString("causation_id")).isEqualTo(metadata1.commandId.toString())
                assertThat(asJson1.getString("correlation_id")).isEqualTo(metadata1.commandId.toString())
                // check activate event
                val asJson2 = eventsList[1]
                // println(asJson2.encodePrettily())
                assertThat(asJson2.getString("state_type")).isEqualTo("Customer")
                assertThat(asJson2.getString("state_id")).isEqualTo(id.toString())
                assertThat(asJson2.getInteger("version")).isEqualTo(2)
                val expectedEvent2 = CustomerEvent.CustomerActivated(cmd1.reason)
                val json2 = asJson2.getString("event_payload")
                val event2 = json.readValue(json2, CustomerEvent::class.java)
                assertThat(expectedEvent2).isEqualTo(event2)
                val causationId2 = asJson1.getString("id")
                assertThat(asJson2.getString("causation_id")).isEqualTo(causationId2)
                assertThat(asJson2.getString("correlation_id")).isEqualTo(metadata1.commandId.toString())
                // check deactivate events
                val asJson3 = eventsList[2]
                // println(asJson3.encodePrettily())
                assertThat(asJson3.getString("state_type")).isEqualTo("Customer")
                assertThat(asJson3.getString("state_id")).isEqualTo(id.toString())
                assertThat(asJson3.getInteger("version")).isEqualTo(3)
                val expectedEvent3 = CustomerEvent.CustomerDeactivated(cmd2.reason)
                val json3 = asJson3.getString("event_payload")
                val event3 = json.readValue(json3, CustomerEvent::class.java)
                assertThat(expectedEvent3).isEqualTo(event3)
                assertThat(asJson3.getString("causation_id")).isEqualTo(metadata2.causationId.toString())
                assertThat(asJson3.getString("correlation_id")).isEqualTo(metadata2.correlationId.toString())
                tc.completeNow()
              }
          }
      }
  }
}
