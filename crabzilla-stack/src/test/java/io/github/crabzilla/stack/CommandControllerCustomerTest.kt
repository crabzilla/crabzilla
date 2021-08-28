// package io.github.crabzilla.stack
//
// import io.github.crabzilla.core.command.CommandControllerConfig
// import io.github.crabzilla.core.command.CommandException.LockingException
// import io.github.crabzilla.core.command.CommandHandler
// import io.github.crabzilla.example1.customer.Customer
// import io.github.crabzilla.example1.customer.CustomerCommand
// import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
// import io.github.crabzilla.example1.customer.CustomerEvent
// import io.github.crabzilla.example1.customer.customerConfig
// import io.github.crabzilla.stack.command.CommandMetadata
// import io.github.crabzilla.stack.command.CommandController
// import io.github.crabzilla.stack.command.SnapshotRepository
// import io.kotest.assertions.fail
// import io.kotest.assertions.shouldFail
// import io.kotest.core.spec.style.BehaviorSpec
// import io.mockk.every
// import io.mockk.mockk
// import io.vertx.core.Future
// import io.vertx.core.Vertx
// import java.util.UUID
//
// // to run from ide: kotest-intellij-plugin
//
// class CommandControllerCustomerTest : BehaviorSpec({
//
//  lateinit var snapshotRepo: SnapshotRepository<Customer>
//  lateinit var eventStore: CommandController<Customer, CustomerCommand, CustomerEvent>
//
//  beforeTest {
//    snapshotRepo = mockk()
//    eventStore = mockk()
//  }
//
//  val eventBus = Vertx.vertx().eventBus()
//
//  Given("mocked snapshotRepo and CommandController") {
//
//    When("I send a register command") {
//
//      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
//      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()
//
//      val controller = CommandController(customerConfig, snapshotRepo, eventStore)
//      val stateId = StateId(UUID.randomUUID())
//      val result = controller
//        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "customer#1"))
//      Then("It should have the expected StatefulSession") {
//        result
//          .onFailure { err -> fail(err.message ?: "wtf?") }
//          .onSuccess { session ->
//            session.originalVersion shouldBe 0
//            session.currentState shouldBe Customer(id = stateId.id, name = "customer#1")
//            session.appliedEvents() shouldContainInOrder
//              listOf(CustomerEvent.CustomerRegistered(id = stateId.id, name = "customer#1"))
//          }
//      }
//    }
//
//    When("I send an invalid register command") {
//
//      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
//      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()
//
//      val controller = CommandController(customerConfig, snapshotRepo, eventStore)
//      val stateId = StateId(UUID.randomUUID())
//      val result = controller
//        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "bad customer"))
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
//        Future.failedFuture(LockingException("Concurrency error"))
//      val controller = CommandController(customerConfig, snapshotRepo, eventStore)
//      val stateId = StateId(UUID.randomUUID())
//      val result = controller
//        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "good customer"))
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
//        Future.failedFuture(LockingException("Concurrency error"))
//      val controller = CommandController(customerConfig, snapshotRepo, eventStore)
//      val stateId = StateId(UUID.randomUUID())
//      val result = controller
//        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "good customer"))
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
//        { commandHandler }, customerConfig.commandValidator
//      )
//      val badController = CommandController(mockedCustomerConfig, snapshotRepo, eventStore)
//      val stateId = StateId(UUID.randomUUID())
//      val result = badController
//        .handle(CommandMetadata(stateId), RegisterCustomer(stateId.id, "good customer"))
//      Then("It should fail") {
//        result
//          .onFailure { it shouldHaveMessage "I got an error!" }
//          .onSuccess {
//            shouldFail { }
//          }
//      }
//    }
//  }
// })
