package io.github.crabzilla.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Persisting events")
class PersistingEventsIT : AbstractCommandIT() {
  @Test
  @DisplayName("appending 1 command with 2 events results in version 2 ")
  fun s1(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    // TODO WIP to migrate a stream to another stream leveraging previous state
    val customer = Customer.Active(UUID.randomUUID(), "c1", "cust#1")
    val objectMapper = ObjectMapper().writerWithDefaultPrettyPrinter()
    println(customer::class.java.simpleName)
    println(objectMapper.writeValueAsString(customer))
    // TODO end

    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd = RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    crabzillaHandler.handle(targetStream1, cmd)
      .compose {
        testRepository.overview()
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepository.scanEvents(0, 10)
          .onFailure { it2 -> tc.failNow(it2) }
          .onSuccess { eventsList ->
            tc.verify {
              assertThat(eventsList.size).isEqualTo(2)
              // check register event
              val asJson1 = eventsList[0]
              val eventId1 = asJson1.getString("id")
              assertThat(asJson1.getInteger("version")).isEqualTo(1)
              assertThat(asJson1.getJsonObject("event_payload").getString("type"))
                .isEqualTo("CustomerRegistered")
              assertThat(asJson1.getString("causation_id")).isEqualTo(eventId1)
              assertThat(asJson1.getString("correlation_id")).isEqualTo(eventId1)

              // check activate event
              val asJson2 = eventsList[1]
              val eventId2 = asJson2.getString("id")
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
  fun s11(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#1", "is needed")
    crabzillaHandler.handle(targetStream1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
        crabzillaHandler.handle(targetStream1, cmd2)
          .compose {
            testRepository.overview()
          }
          .onFailure { err -> tc.failNow(err) }
          .onSuccess {
            testRepository.scanEvents(0, 10)
              .onFailure { err -> tc.failNow(err) }
              .onSuccess { eventsList: List<JsonObject> ->
                tc.verify {
                  assertThat(eventsList.size).isEqualTo(3)
                  // check register event
                  val asJson1 = eventsList[0]
                  val eventId1 = asJson1.getString("id")
                  assertThat(asJson1.getInteger("version")).isEqualTo(1)
                  assertThat(asJson1.getJsonObject("event_payload").getString("type"))
                    .isEqualTo("CustomerRegistered")
                  assertThat(asJson1.getString("causation_id")).isEqualTo(eventId1)
                  assertThat(asJson1.getString("correlation_id")).isEqualTo(eventId1)

                  // check register private event
                  val asJson2 = eventsList[1]
                  val eventId2 = asJson2.getString("id")
                  assertThat(asJson2.getInteger("version")).isEqualTo(2)
                  assertThat(asJson2.getJsonObject("event_payload").getString("type"))
                    .isEqualTo("CustomerActivated")
                  assertThat(asJson2.getString("causation_id")).isEqualTo(eventId1)
                  assertThat(asJson2.getString("correlation_id")).isEqualTo(eventId1)

                  // check deactivate events
                  val asJson3 = eventsList[2]
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
