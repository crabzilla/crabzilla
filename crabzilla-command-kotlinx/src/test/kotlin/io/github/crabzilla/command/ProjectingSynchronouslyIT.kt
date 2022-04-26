package io.github.crabzilla.command

import io.github.crabzilla.TestsFixtures.json
import io.github.crabzilla.TestsFixtures.pgPool
import io.github.crabzilla.TestsFixtures.testRepo
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomersEventProjector
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.CommandControllerOptions
import io.github.crabzilla.stack.CommandMetadata
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
@DisplayName("Projecting to view model synchronously")
class ProjectingSynchronouslyIT {

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can project to view model synchronously`(vertx: Vertx, tc: VertxTestContext) {

    val repository = KotlinxCommandRepository(json)
    val options = CommandControllerOptions(eventProjector = CustomersEventProjector())
    val controller = CommandController(vertx, pgPool, customerComponent, repository, options)

    val id = UUID.randomUUID()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata.new(id)

    val cmd2 = CustomerCommand.DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata.new(id)

    controller.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(metadata2, cmd2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            testRepo.getAllCustomers()
              .onFailure { tc.failNow(it) }
              .onSuccess { customersList ->
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
