package io.github.crabzilla.stack

import io.github.crabzilla.core.CommandHandler
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.stack.CommandException.OptimisticLockingException
import io.kotest.assertions.fail
import io.kotest.assertions.shouldFail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.every
import io.mockk.mockk
import io.vertx.core.Future
import java.util.UUID

// to run from ide: kotest-intellij-plugin

class ControllerTests : BehaviorSpec({

  lateinit var snapshotRepo: SnapshotRepository<Customer>
  lateinit var eventStore: EventStore<Customer, CustomerCommand, CustomerEvent>

  beforeTest {
    snapshotRepo = mockk()
    eventStore = mockk()
  }

  Given("a controller for Customer") {

    every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
    every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()

    val controller = CommandController(customerConfig, snapshotRepo, eventStore)

    When("I send a register command") {
      val aggregateRootId = AggregateRootId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(aggregateRootId), RegisterCustomer(aggregateRootId.id, "customer#1"))
      Then("It should have the expected StatefulSession") {
        result
          .onFailure { err -> fail(err.message ?: "wtf?") }
          .onSuccess { session ->
            session.originalVersion shouldBe 0
            session.currentState shouldBe Customer(id = aggregateRootId.id, name = "customer#1")
            session.appliedEvents() shouldContainInOrder
              listOf(CustomerEvent.CustomerRegistered(id = aggregateRootId.id, name = "customer#1"))
          }
      }
    }

    When("I send an invalid register command") {
      val aggregateRootId = AggregateRootId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(aggregateRootId), RegisterCustomer(aggregateRootId.id, "bad customer"))
      Then("It should fail") {
        result
          .onFailure { it shouldHaveMessage "[Bad customer!]" }
          .onSuccess {
            shouldFail { }
          }
      }
    }

    When("I send a register command but with Concurrency") {
      every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
      every { eventStore.append(any(), any(), any()) } returns Future.failedFuture(OptimisticLockingException("Concurrency error"))
      val controller =
        CommandController(customerConfig, snapshotRepo, eventStore)
      val aggregateRootId = AggregateRootId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(aggregateRootId), RegisterCustomer(aggregateRootId.id, "good customer"))
      Then("It should fail") {
        result
          .onFailure { it shouldHaveMessage "Concurrency error" }
          .onSuccess {
            shouldFail { }
          }
      }
    }

    When("I send a register command but with error on get") {
      every { snapshotRepo.get(any()) } returns Future.failedFuture("db is down!")
      every { eventStore.append(any(), any(), any()) } returns Future.failedFuture(OptimisticLockingException("Concurrency error"))
      val controller =
        CommandController(customerConfig, snapshotRepo, eventStore)
      val aggregateRootId = AggregateRootId(UUID.randomUUID())
      val result = controller
        .handle(CommandMetadata(aggregateRootId), RegisterCustomer(aggregateRootId.id, "good customer"))
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
      val mockedCustomerConfig = AggregateRootConfig(
        customerConfig.name, customerConfig.eventHandler,
        customerConfig.commandValidator, commandHandler
      )
      val badController = CommandController(mockedCustomerConfig, snapshotRepo, eventStore)
      val aggregateRootId = AggregateRootId(UUID.randomUUID())
      val result = badController
        .handle(CommandMetadata(aggregateRootId), RegisterCustomer(aggregateRootId.id, "good customer"))
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
