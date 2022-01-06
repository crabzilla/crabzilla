package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.example1.payment.FuturePaymentCommandHandler
import io.github.crabzilla.example1.payment.Payment
import io.github.crabzilla.example1.payment.PaymentCommand
import io.github.crabzilla.example1.payment.PaymentEvent
import io.github.crabzilla.example1.payment.paymentEventHandler
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.command.internal.PersistentSnapshotRepo
import io.kotest.matchers.shouldBe
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
class FutureCommandHandlerIT {

  private lateinit var jsonSerDer: JsonSerDer
  private lateinit var commandsContext: CommandsContext
  private lateinit var commandController: CommandController<Payment, PaymentCommand, PaymentEvent>
  private lateinit var repository: SnapshotTestRepository<Payment>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    jsonSerDer = KotlinJsonSerDer(example1Json)
    commandsContext = CommandsContext.create(vertx, jsonSerDer, connectOptions, poolOptions)
    val paymentConfig = CommandControllerConfig(
      "Payment",
      paymentEventHandler,
      { FuturePaymentCommandHandler(paymentEventHandler, vertx.eventBus()) }
    )
    val snapshotRepo2 = PersistentSnapshotRepo<Payment, PaymentEvent>(customerConfig.name, jsonSerDer)
    commandController =
      CommandController(vertx, commandsContext.pgPool, jsonSerDer, paymentConfig, snapshotRepo2,)
    repository = SnapshotTestRepository(commandsContext.pgPool, example1Json)
    testRepo = TestRepository(commandsContext.pgPool)
    cleanDatabase(commandsContext.pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can hadle an async command handler")
  fun s1(tc: VertxTestContext) {
    val stateId = StateId(UUID.randomUUID())
    val cmd = PaymentCommand.Pay(stateId.id, "000", 10.00)
    val metadata = CommandMetadata(stateId)
    commandController.handle(metadata, cmd)
      .onFailure { err ->
        tc.failNow(err)
      }
      .onSuccess { sideEffects ->
        sideEffects.appendedEvents.size shouldBe 2
        tc.completeNow()
      }
  }
}
