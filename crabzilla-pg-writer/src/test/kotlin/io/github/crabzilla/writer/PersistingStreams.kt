package io.github.crabzilla.writer

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistingStreams : AbstractCrabzillaWriterIT() {
  @Test
  @DisplayName("migrating streams - TODO ")
  @Disabled
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
  }

  @Test
  fun `it leverages stateType and stateId to form it's name`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd = CustomerCommand.RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    crabzillaWriter.handle(targetStream, cmd)
      .compose { testRepository.getStreams() }
      .onFailure { tc.failNow(it) }
      .onSuccess { streams ->
        tc.verify {
          assertThat(streams.size).isEqualTo(1)
          val stream = streams.first()
          assertThat(stream.getString("state_type")).isEqualTo(targetStream.stateType)
          assertThat(stream.getString("state_id")).isEqualTo(targetStream.stateId)
          assertThat(stream.getString("name")).isEqualTo("Customer@${targetStream.stateId}")
          assertThat(stream.getString("status")).isEqualTo("OPEN")
        }
        tc.completeNow()
      }
  }

  // TODO add a test for mandatory @ within stream name
  @Test
  fun `it name can be specified too`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(name = "customer@123", stateType = "Customer", stateId = customerId1.toString())
    val cmd = CustomerCommand.RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    crabzillaWriter.handle(targetStream, cmd)
      .compose { testRepository.getStreams() }
      .onFailure { tc.failNow(it) }
      .onSuccess { streams ->
        tc.verify {
          assertThat(streams.size).isEqualTo(1)
          val stream = streams.first()
          assertThat(stream.getString("state_type")).isEqualTo(targetStream.stateType)
          assertThat(stream.getString("state_id")).isEqualTo(targetStream.stateId)
          assertThat(stream.getString("name")).isEqualTo("customer@123")
          assertThat(stream.getString("status")).isEqualTo("OPEN")
        }
        tc.completeNow()
      }
  }

  @Test
  fun `if it must be new, there is a check`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(name = "customer@123", mustBeNew = true)
    val cmd = CustomerCommand.RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    crabzillaWriter.handle(targetStream, cmd)
      .compose { crabzillaWriter.handle(targetStream, cmd) }
      .onFailure {
        testRepository.getStreams()
          .onSuccess { streams ->
            tc.verify {
              assertThat(streams.size).isEqualTo(1)
              val stream = streams.first()
              assertThat(stream.getString("state_type")).isEqualTo(targetStream.stateType)
              assertThat(stream.getString("state_id")).isEqualTo(targetStream.stateId)
              assertThat(stream.getString("name")).isEqualTo("customer@123")
              assertThat(stream.getString("status")).isEqualTo("OPEN")
            }
            tc.completeNow()
          }
          .onFailure {
            tc.failNow(it)
          }
      }
      .onSuccess {
        tc.failNow("Should fail on second command")
      }
  }
}
