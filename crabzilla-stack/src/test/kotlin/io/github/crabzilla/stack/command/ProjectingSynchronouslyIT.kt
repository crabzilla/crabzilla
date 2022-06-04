package io.github.crabzilla.stack.command

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerComponent
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
class ProjectingSynchronouslyIT: AbstractCommandIT() {

  @Test
  fun `it can project to view model synchronously`(vertx: Vertx, tc: VertxTestContext) {
    val options = CommandServiceOptions(eventProjector = CustomersEventProjector())
    val service = factory.commandService(customerComponent, jsonSerDer, options)

    val id = UUID.randomUUID()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")

    service.handle(id, cmd1)
      .compose {
        service.handle(id, cmd2)
      }
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.getAllCustomers()
          .onFailure { tc.failNow(it) }
          .onSuccess { customersList ->
            tc.verify {
              assertThat(customersList.size).isEqualTo(1)
              val json = customersList.first()
              assertThat(UUID.fromString(json.getString("id"))).isEqualTo(id)
              assertThat(json.getString("name")).isEqualTo(cmd1.name)
              assertThat(json.getBoolean("is_active")).isEqualTo(false)
              tc.completeNow()
            }
          }
      }
  }
}
