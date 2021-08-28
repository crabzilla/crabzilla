package io.github.crabzilla.engine

import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.engine.command.CommandController
import io.github.crabzilla.engine.command.CommandsContext
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.example1.payment.FuturePaymentCommandHandler
import io.github.crabzilla.example1.payment.Payment
import io.github.crabzilla.example1.payment.PaymentCommand
import io.github.crabzilla.example1.payment.PaymentEvent
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRequested
import io.github.crabzilla.example1.payment.Status
import io.github.crabzilla.example1.payment.paymentEventHandler
import io.github.crabzilla.serder.KotlinSerDer
import io.github.crabzilla.serder.SerDer
import io.github.crabzilla.stack.StateId
import io.github.crabzilla.stack.command.CommandMetadata
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

  private lateinit var serDer: SerDer
  private lateinit var client: CommandsContext
  private lateinit var controller: CommandController<Payment, PaymentCommand, PaymentEvent>
  private lateinit var repository: SnapshotRepository<Payment>
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    serDer = KotlinSerDer(example1Json)
    client = CommandsContext.create(vertx, serDer, connectOptions, poolOptions)
    val paymentConfig = CommandControllerConfig(
      "Payment",
      paymentEventHandler,
      { FuturePaymentCommandHandler(paymentEventHandler, vertx.eventBus()) }
    )
    controller = CommandController(vertx, paymentConfig, client.pgPool, serDer, true)
    repository = SnapshotRepository(client.pgPool, example1Json)
    testRepo = TestRepository(client.pgPool)
    cleanDatabase(client.sqlClient)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  @DisplayName("it can hadle an async command handler")
  fun s1(tc: VertxTestContext) {
    val stateId = StateId(UUID.randomUUID())
    val cmd = PaymentCommand.Pay(stateId.id, "000", 10.00)
    val metadata = CommandMetadata(stateId)
    controller.handle(metadata, cmd)
      .onFailure { err ->
        tc.failNow(err)
      }
      .onSuccess { session ->
        session.originalVersion shouldBe 0
        session.currentState shouldBe
          Payment(stateId.id, "000", 10.00, Status.Approved, "ok")
        session.appliedEvents() shouldBe
          listOf(
            PaymentRequested(stateId.id, "000", 10.00),
            PaymentApproved("ok")
          )
        tc.completeNow()
      }
  }
}
