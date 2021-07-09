package io.github.crabzilla.stack

import io.github.crabzilla.example1.payment.Payment
import io.github.crabzilla.example1.payment.PaymentCommand
import io.github.crabzilla.example1.payment.PaymentEvent
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentApproved
import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRequested
import io.github.crabzilla.example1.payment.Status
import io.github.crabzilla.example1.payment.paymentConfig
import io.github.crabzilla.stack.command.CommandController
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.storage.EventStore
import io.github.crabzilla.stack.storage.SnapshotRepository
import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.math.BigDecimal
import java.util.UUID

// to run from ide: kotest-intellij-plugin

class CommandControllerPaymentTest : BehaviorSpec({

  lateinit var snapshotRepo: SnapshotRepository<Payment>
  lateinit var eventStore: EventStore<Payment, PaymentCommand, PaymentEvent>

  beforeTest {
    snapshotRepo = mockk()
    eventStore = mockk()
  }

  val eventBus = Vertx.vertx().eventBus()

  Given("mocked snapshotRepo and EventStore") {

    When("I send a pay command") {
      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()
      val controller = CommandController(paymentConfig, snapshotRepo, eventStore, eventBus)
      val domainStateId = DomainStateId(UUID.randomUUID())
      val result = controller
        .handle(
          CommandMetadata(domainStateId),
          PaymentCommand.Pay(domainStateId.id, "000", BigDecimal(10))
        )
      Then("It should have the expected StatefulSession") {
        result
          .onFailure { err -> fail(err.message ?: "wtf?") }
          .onSuccess { session ->
            session.originalVersion shouldBe 0
            session.currentState shouldBe
              Payment(domainStateId.id, "000", BigDecimal(10), Status.Approved, "ok")
            session.appliedEvents() shouldBe
              listOf(
                PaymentRequested(domainStateId.id, "000", BigDecimal(10)),
                PaymentApproved("ok")
              )
          }
      }
    }

//    When("I send an invalid register command") {
//      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
//      val domainStateId = DomainStateId(UUID.randomUUID())
//      val result = controller
//        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "bad customer"))
//      Then("It should fail") {
//        result
//          .onFailure { it shouldHaveMessage "[Bad customer!]" }
//          .onSuccess {
//            shouldFail { }
//          }
//      }
//    }
//
//    When("I send a register command but with Concurrency") {
//      every { snapshotRepo.get(any()) } returns
//        Future.succeededFuture(null)
//      every { eventStore.append(any(), any(), any()) } returns
//        Future.failedFuture(OptimisticLockingException("Concurrency error"))
//      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
//      val domainStateId = DomainStateId(UUID.randomUUID())
//      val result = controller
//        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "good customer"))
//      Then("It should fail") {
//        result
//          .onFailure { it shouldHaveMessage "Concurrency error" }
//          .onSuccess {
//            shouldFail { }
//          }
//      }
//    }
//
//    When("I send a register command but with error on get") {
//      every { snapshotRepo.get(any()) } returns
//        Future.failedFuture("db is down!")
//      every { eventStore.append(any(), any(), any()) } returns
//        Future.failedFuture(OptimisticLockingException("Concurrency error"))
//      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
//      val domainStateId = DomainStateId(UUID.randomUUID())
//      val result = controller
//        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "good customer"))
//      Then("It should fail") {
//        result
//          .onFailure { it shouldHaveMessage "db is down!" }
//          .onSuccess {
//            shouldFail { }
//          }
//      }
//    }
//
//    When("I send a register command but with error on handle") {
//      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
//      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()
//      val commandHandler: CommandHandler<Customer, CustomerCommand, CustomerEvent> = mockk()
//      every { commandHandler.handleCommand(any(), any(), any()) } throws RuntimeException("I got an error!")
//      val mockedCustomerConfig = CommandControllerConfig(
//        customerConfig.name, customerConfig.eventHandler,
//        commandHandler, customerConfig.commandValidator
//      )
//      val badController = CommandController(mockedCustomerConfig, snapshotRepo, eventStore, eventBus)
//      val domainStateId = DomainStateId(UUID.randomUUID())
//      val result = badController
//        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "good customer"))
//      Then("It should fail") {
//        result
//          .onFailure { it shouldHaveMessage "I got an error!" }
//          .onSuccess {
//            shouldFail { }
//          }
//      }
//    }
  }
})
