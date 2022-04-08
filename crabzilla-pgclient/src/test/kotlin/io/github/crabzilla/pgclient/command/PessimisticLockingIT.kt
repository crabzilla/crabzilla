package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.example1Json
import io.github.crabzilla.pgclient.TestRepository
import io.vertx.core.CompositeFuture
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Running concurrent commands should pessimistically lock")
class PessimisticLockingIT {

  private lateinit var commandController: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    val pgPool = pgPool(vertx)
    commandController = CommandController(vertx, pgPool, example1Json, customerConfig)
    testRepo = TestRepository(pgPool)
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `lock works`(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = CustomerCommand.RegisterCustomer(id, "good customer")
    val metadata = CommandMetadata.new(id)
    val future1 = commandController.handle(metadata, cmd)
    val future2 = commandController.handle(metadata, cmd)
    CompositeFuture.all(future1, future2)
      .onSuccess {
        tc.failNow("it should fail")
      }
      .onFailure {
        tc.verify {
          assertEquals(it.javaClass.simpleName, "LockingException")
          tc.completeNow()
        }
      }
  }
}
