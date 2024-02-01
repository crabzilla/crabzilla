package io.github.crabzilla.writer

import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommandSerDer
import io.github.crabzilla.example1.customer.CustomerEventSerDer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerCommandHandler
import io.github.crabzilla.example1.customer.customerEventHandler
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
@DisplayName("Projecting to view model synchronously")
class ProjectingSynchronouslyIT : AbstractCrabzillaHandlerIT() {
  @Test
  fun `it can project to view model synchronously`(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    val customerConfig =
      CrabzillaWriterConfig(
        initialState = Customer.Initial,
        eventHandler = customerEventHandler,
        commandHandler = customerCommandHandler,
        commandSerDer = CustomerCommandSerDer(),
        eventSerDer = CustomerEventSerDer(),
        eventProjector = CustomersEventProjector(),
      )

    val service = CrabzillaWriterImpl(context, customerConfig)

    val customerId1 = UUID.randomUUID()
    val targetStream1 = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    service.handle(targetStream1, cmd1)
      .compose {
        service.handle(targetStream1, cmd2)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepository.getAllCustomers()
          .onFailure { tc.failNow(it) }
          .onSuccess { customersList ->
            tc.verify {
              assertThat(customersList.size).isEqualTo(1)
              val json = customersList.first()
              assertThat(json.getString("id")).isEqualTo(targetStream1.stateId)
              assertThat(json.getString("name")).isEqualTo(cmd1.name)
              assertThat(json.getBoolean("is_active")).isEqualTo(false)
              tc.completeNow()
            }
          }
      }
  }
}
