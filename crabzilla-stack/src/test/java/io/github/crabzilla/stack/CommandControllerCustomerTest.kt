package io.github.crabzilla.stack

import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.stack.command.CommandController
import io.github.crabzilla.stack.command.CommandControllerConfig
import io.github.crabzilla.stack.command.CommandException.OptimisticLockingException
import io.github.crabzilla.stack.command.CommandMetadata
import io.github.crabzilla.stack.command.EventStore
import io.github.crabzilla.stack.command.SnapshotRepository
import io.kotest.assertions.fail
import io.kotest.assertions.shouldFail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.every
import io.mockk.mockk
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.util.UUID

// to run from ide: kotest-intellij-plugin

class CommandControllerCustomerTest : BehaviorSpec({

  lateinit var snapshotRepo: SnapshotRepository<Customer>
  lateinit var eventStore: EventStore<Customer, CustomerCommand, CustomerEvent>

  beforeTest {
    snapshotRepo = mockk()
    eventStore = mockk()
  }

  val eventBus = Vertx.vertx().eventBus()

  Given("mocked snapshotRepo and EventStore") {

    When("I send a register command") {

      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()

      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
      val domainStateId = DomainStateId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "customer#1"))
      Then("It should have the expected StatefulSession") {
        result
          .onFailure { err -> fail(err.message ?: "wtf?") }
          .onSuccess { session ->
            session.originalVersion shouldBe 0
            session.currentState shouldBe Customer(id = domainStateId.id, name = "customer#1")
            session.appliedEvents() shouldContainInOrder
              listOf(CustomerEvent.CustomerRegistered(id = domainStateId.id, name = "customer#1"))
          }
      }
    }

    When("I send an invalid register command") {

      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()

      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
      val domainStateId = DomainStateId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "bad customer"))
      Then("It should fail") {
        result
          .onFailure { it shouldHaveMessage "[Bad customer!]" }
          .onSuccess {
            shouldFail { }
          }
      }
    }

    When("I send a register command but with Concurrency") {
      every { snapshotRepo.get(any()) } returns
        Future.succeededFuture(null)
      every { eventStore.append(any(), any(), any()) } returns
        Future.failedFuture(OptimisticLockingException("Concurrency error"))
      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
      val domainStateId = DomainStateId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "good customer"))
      Then("It should fail") {
        result
          .onFailure { it shouldHaveMessage "Concurrency error" }
          .onSuccess {
            shouldFail { }
          }
      }
    }

    When("I send a register command but with error on get") {
      every { snapshotRepo.get(any()) } returns
        Future.failedFuture("db is down!")
      every { eventStore.append(any(), any(), any()) } returns
        Future.failedFuture(OptimisticLockingException("Concurrency error"))
      val controller = CommandController(customerConfig, snapshotRepo, eventStore, eventBus)
      val domainStateId = DomainStateId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "good customer"))
      Then("It should fail") {
        result
          .onFailure { it shouldHaveMessage "db is down!" }
          .onSuccess {
            shouldFail { }
          }
      }
    }

    When("I send a register command but with error on handle") {
      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
      every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()
      val commandHandler: CommandHandler<Customer, CustomerCommand, CustomerEvent> = mockk()
      every { commandHandler.handleCommand(any(), any(), any()) } throws RuntimeException("I got an error!")
      val mockedCustomerConfig = CommandControllerConfig(
        customerConfig.name, customerConfig.eventHandler,
        commandHandler, customerConfig.commandValidator
      )
      val badController = CommandController(mockedCustomerConfig, snapshotRepo, eventStore, eventBus)
      val domainStateId = DomainStateId(UUID.randomUUID())
      val result = badController
        .handle(CommandMetadata(domainStateId), RegisterCustomer(domainStateId.id, "good customer"))
      Then("It should fail") {
        result
          .onFailure { it shouldHaveMessage "I got an error!" }
          .onSuccess {
            shouldFail { }
          }
      }
    }
  }
})
