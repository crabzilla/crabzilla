package io.github.crabzilla.pgc

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.pgc.command.CommandController
import io.github.crabzilla.pgc.command.CommandsContext
import io.github.crabzilla.stack.DomainStateId
import io.github.crabzilla.stack.command.CommandMetadata
import io.kotest.matchers.throwable.shouldHaveMessage
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandsValidationIT {

  private lateinit var client: CommandsContext
  private lateinit var eventStore: CommandController<Customer, CustomerCommand, CustomerEvent>
  private lateinit var repository: SnapshotRepository<Customer>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    client = CommandsContext.create(vertx, example1Json, connectOptions, poolOptions)
    eventStore = CommandController(vertx, customerConfig, client.pgPool, client.json, true)
    repository = SnapshotRepository(client.pgPool, client.json)
    testRepo = TestRepository(client.pgPool)
    cleanDatabase(client.sqlClient)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can validate command")
  fun s1(tc: VertxTestContext) {
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "bad customer", "is needed")
    val metadata = CommandMetadata(DomainStateId(id))
    eventStore.handle(metadata, cmd)
      .onFailure {
        it shouldHaveMessage "[Bad customer!]"
        tc.completeNow()
      }
      .onSuccess {
        tc.failNow("It should fail")
      }
  }
}
