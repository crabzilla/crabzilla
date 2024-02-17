package io.github.crabzilla.writer

import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.stream.TargetStream
import io.vertx.core.Vertx
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
@DisplayName("Writer returns a StreamSnapshot")
class AssertingWriterResult : AbstractWriterApiIT() {
  @Test
  fun `after 2 commands, the result is correct`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    writerApi.handle(targetStream1, cmd1)
      .compose {
        writerApi.handle(targetStream1, cmd2)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess { result ->
        tc.verify {
          val expectedState = Customer.Inactive(customerId1, cmd1.name, "it's not needed anymore")
          assertThat(result.snapshot.state).isEqualTo(expectedState)
          assertThat(result.snapshot.version).isEqualTo(3)
          tc.completeNow()
        }
      }
  }
}
