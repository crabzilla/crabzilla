package io.github.crabzilla.command

import io.github.crabzilla.CrabzillaContext
import io.github.crabzilla.TestRepository
import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerJsonObjectSerDer
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.testDbConfig
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
class PersistingEventsIT {

  private lateinit var context : CrabzillaContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaContext.new(vertx, testDbConfig)
    testRepo = TestRepository(context.pgPool)
    cleanDatabase(context.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("appending 1 command with 2 events results in version 2 ")
  fun s1(tc: VertxTestContext, vertx: Vertx) {
    val controller = context.featureController(customerComponent, jsonSerDer)
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata.new(id)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.scanEvents(0, 10)
          .onFailure { it2 -> tc.failNow(it2) }
          .onSuccess { eventsList ->
            tc.verify {
              assertThat(eventsList.size).isEqualTo(2)
              // check register event
              val asJson1 = eventsList[0]
              val eventId1 = asJson1.getString("id")
              assertThat(asJson1.getString("state_type")).isEqualTo("Customer")
              assertThat(asJson1.getString("state_id")).isEqualTo(id.toString())
              assertThat(asJson1.getInteger("version")).isEqualTo(1)
              assertThat(asJson1.getJsonObject("event_payload").getString("type"))
                .isEqualTo("CustomerRegistered")
              assertThat(asJson1.getString("causation_id")).isEqualTo(eventId1)
              assertThat(asJson1.getString("correlation_id")).isEqualTo(eventId1)
              // check activate event
              val asJson2 = eventsList[1]
              assertThat(asJson2.getString("state_type")).isEqualTo("Customer")
              assertThat(asJson2.getString("state_id")).isEqualTo(id.toString())
              assertThat(asJson2.getInteger("version")).isEqualTo(2)
              assertThat(asJson2.getJsonObject("event_payload").getString("type"))
                .isEqualTo("CustomerActivated")
              assertThat(asJson2.getString("causation_id")).isEqualTo(eventId1)
              assertThat(asJson2.getString("correlation_id")).isEqualTo(eventId1)
              tc.completeNow()
            }
          }
      }
  }

  @Test
  @DisplayName("appending 2 commands with 2 and 1 event, respectively results in version 3")
  fun s11(tc: VertxTestContext, vertx: Vertx) {
    val jsonSerDer = CustomerJsonObjectSerDer()
    val controller = context.featureController(customerComponent, jsonSerDer)
    val id = UUID.randomUUID()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata.new(id)
    controller.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess { sideEffect1: CommandSideEffect ->
        val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
        val metadata2 = CommandMetadata.new(id)
        controller.handle(metadata2, cmd2)
          .onFailure { err -> tc.failNow(err) }
          .onSuccess {
            testRepo.scanEvents(0, 10)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { eventsList: List<JsonObject> ->
                eventsList.forEachIndexed { index, it ->
                  println("$index -> ${it.encodePrettily()}")
                }
                tc.verify {
                  assertThat(eventsList.size).isEqualTo(3)
                  // check register event
                  val asJson1 = eventsList[0]
                  // println(asJson1.encodePrettily())
                  val eventId1 = asJson1.getString("id")
                  assertThat(asJson1.getString("state_type")).isEqualTo("Customer")
                  assertThat(asJson1.getString("state_id")).isEqualTo(id.toString())
                  assertThat(asJson1.getInteger("version")).isEqualTo(1)
                  assertThat(asJson1.getJsonObject("event_payload").getString("type"))
                    .isEqualTo("CustomerRegistered")
                  assertThat(asJson1.getString("causation_id")).isEqualTo(eventId1)
                  assertThat(asJson1.getString("correlation_id")).isEqualTo(eventId1)
                  // check activate event
                  val asJson2 = eventsList[1]
                  // println(asJson2.encodePrettily())
                  val eventId2 = asJson2.getString("id")
                  assertThat(asJson2.getString("state_type")).isEqualTo("Customer")
                  assertThat(asJson2.getString("state_id")).isEqualTo(id.toString())
                  assertThat(asJson2.getInteger("version")).isEqualTo(2)
                  assertThat(asJson2.getJsonObject("event_payload").getString("type"))
                    .isEqualTo("CustomerActivated")
                  assertThat(asJson2.getString("causation_id")).isEqualTo(eventId1)
                  assertThat(asJson2.getString("correlation_id")).isEqualTo(eventId1)
                  // check deactivate events
                  val asJson3 = eventsList[2]
                  // println(asJson3.encodePrettily())
                  assertThat(asJson3.getString("state_type")).isEqualTo("Customer")
                  assertThat(asJson3.getString("state_id")).isEqualTo(id.toString())
                  assertThat(asJson3.getInteger("version")).isEqualTo(3)
                  assertThat(asJson3.getJsonObject("event_payload").getString("type"))
                    .isEqualTo("CustomerDeactivated")
                  assertThat(asJson3.getString("causation_id")).isEqualTo(eventId2)
                  assertThat(asJson3.getString("correlation_id")).isEqualTo(eventId1)
                  tc.completeNow()
                }
              }
          }
      }
  }
}
