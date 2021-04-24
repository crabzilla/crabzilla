package io.github.crabzilla.core

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.stack.AggregateRootId
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.EventStore
import io.github.crabzilla.stack.SnapshotRepository
import io.kotest.assertions.fail
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.vertx.core.Future

// to run from ide: kotest-intellij-plugin

class ControllerTests : BehaviorSpec({

  lateinit var snapshotRepo: SnapshotRepository<Customer, CustomerCommand, CustomerEvent>
  lateinit var eventStore: EventStore<Customer, CustomerCommand, CustomerEvent>

  beforeTest {
    snapshotRepo = mockk()
    eventStore = mockk()
  }

  Given("a controller for Customer") {

    every { snapshotRepo.get(any()) } returns Future.succeededFuture(null)
    every { eventStore.append(any(), any(), any()) } returns Future.succeededFuture()

    val controller =
      CommandController(customerConfig.commandValidator, customerConfig.commandHandler, snapshotRepo, eventStore)

    When("I send a register command") {
      val aggregateRootId = AggregateRootId()
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
  }
})
