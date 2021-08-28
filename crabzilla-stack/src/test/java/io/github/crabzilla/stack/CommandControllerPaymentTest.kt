// package io.github.crabzilla.stack
//
// import io.github.crabzilla.core.command.CommandControllerConfig
// import io.github.crabzilla.example1.payment.FuturePaymentCommandHandler
// import io.github.crabzilla.example1.payment.Payment
// import io.github.crabzilla.example1.payment.PaymentCommand
// import io.github.crabzilla.example1.payment.PaymentEvent
// import io.github.crabzilla.example1.payment.PaymentEvent.PaymentApproved
// import io.github.crabzilla.example1.payment.PaymentEvent.PaymentRequested
// import io.github.crabzilla.example1.payment.Status
// import io.github.crabzilla.example1.payment.paymentEventHandler
// import io.github.crabzilla.stack.command.CommandMetadata
// import io.github.crabzilla.stack.command.CommandController
// import io.github.crabzilla.stack.command.SnapshotRepository
// import io.kotest.assertions.fail
// import io.kotest.core.spec.style.BehaviorSpec
// import io.mockk.every
// import io.mockk.mockk
// import io.vertx.core.Future
// import io.vertx.core.Vertx
// import java.util.UUID
//
// // to run from ide: kotest-intellij-plugin
//
// class CommandControllerPaymentTest : BehaviorSpec({
//
//  lateinit var snapshotRepo: SnapshotRepository<Payment>
//  lateinit var eventStore: CommandController<Payment, PaymentCommand, PaymentEvent>
//
//  beforeTest {
//    snapshotRepo = mockk()
//    eventStore = mockk()
//  }
//
//  val vertx = Vertx.vertx()
//  val eventBus = vertx.eventBus()
//
//  Given("mocked snapshotRepo and CommandController") {
//
//    val paymentConfig = CommandControllerConfig(
//      "Payment",
//      paymentEventHandler,
//      { FuturePaymentCommandHandler(eventBus) }
//    )
//
//    When("I send a pay command") {
//      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
//      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()
//      val controller = CommandController(paymentConfig, snapshotRepo, eventStore)
//      val stateId = StateId(UUID.randomUUID())
//      val result = controller
//        .handle(
//          CommandMetadata(stateId),
//          PaymentCommand.Pay(stateId.id, "000", 10.00)
//        )
//      Then("It should have the expected StatefulSession") {
//        result
//          .onFailure { err -> fail(err.message ?: "wtf?") }
//          .onSuccess { session ->
//            session.originalVersion shouldBe 0
//            session.currentState shouldBe
//              Payment(stateId.id, "000", 10.00, Status.Approved, "ok")
//            session.appliedEvents() shouldBe
//              listOf(
//                PaymentRequested(stateId.id, "000", 10.00),
//                PaymentApproved("ok")
//              )
//          }
//      }
//    }
//
// //    When("I send an invalid register command") {
// //      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
// //      val stateId = StateId(UUID.randomUUID())
// //      val result = controller
// //        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "bad customer"))
// //      Then("It should fail") {
// //        result
// //          .onFailure { it shouldHaveMessage "[Bad customer!]" }
// //          .onSuccess {
// //            shouldFail { }
// //          }
// //      }
// //    }
// //
// //    When("I send a register command but with Concurrency") {
// //      every { snapshotRepo.get(any()) } returns
// //        Future.succeededFuture(null)
// //      every { eventStore.append(any(), any(), any()) } returns
// //        Future.failedFuture(LockingException("Concurrency error"))
// //      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
// //      val stateId = StateId(UUID.randomUUID())
// //      val result = controller
// //        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "good customer"))
// //      Then("It should fail") {
// //        result
// //          .onFailure { it shouldHaveMessage "Concurrency error" }
// //          .onSuccess {
// //            shouldFail { }
// //          }
// //      }payment.kt
// //    }
// //
// //    When("I send a register command but with error on get") {
// //      every { snapshotRepo.get(any()) } returns
// //        Future.failedFuture("db is down!")
// //      every { eventStore.append(any(), any(), any()) } returns
// //        Future.failedFuture(LockingException("Concurrency error"))
// //      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
// //      val stateId = StateId(UUID.randomUUID())
// //      val result = controller
// //        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "good customer"))
// //      Then("It should fail") {
// //        result
// //          .onFailure { it shouldHaveMessage "db is down!" }
// //          .onSuccess {
// //            shouldFail { }
// //          }
// //      }
// //    }
// //
// //    When("I send a register command but with error on handle") {
// //      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
// //      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()
// //      val commandHandler: CommandHandler<Customer, CustomerCommand, CustomerEvent> = mockk()
// //      every { commandHandler.handleCommand(any(), any(), any()) } throws RuntimeException("I got an error!")
// //      val mockedCustomerConfig = CommandControllerConfig(
// //        customerConfig.name, customerConfig.eventHandler,
// //        commandHandler, customerConfig.commandValidator
// //      )
// //      val badController = CommandController(mockedCustomerConfig, snapshotRepo, eventStore, eventBus)
// //      val stateId = StateId(UUID.randomUUID())
// //      val result = badController
// //        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "good customer"))
// //      Then("It should fail") {
// //        result
// //          .onFailure { it shouldHaveMessage "I got an error!" }
// //          .onSuccess {
// //            shouldFail { }
// //          }
// //      }
// //    }
//  }
// })
