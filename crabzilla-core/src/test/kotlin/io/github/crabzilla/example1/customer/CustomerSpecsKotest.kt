package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.Session
import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.example1.customer.model.Customer.Active
import io.github.crabzilla.example1.customer.model.Customer.Inactive
import io.github.crabzilla.example1.customer.model.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RenameCustomer
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.model.CustomerEvent.CustomerRenamed
import io.github.crabzilla.example1.customer.model.customerDecideFunction
import io.github.crabzilla.example1.customer.model.customerEvolveFunction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import java.util.*

class CustomerSpecsKotest : BehaviorSpec({
  val id = UUID.randomUUID()
  val session =
    Session(
      initialState = Customer.Initial,
      evolveFunction = customerEvolveFunction,
      decideFunction = customerDecideFunction,
    )

  Given("a CustomerRegistered event") {
    val event = CustomerRegistered(id, "c1")
    When("it's applied") {
      session.reset().evolve(event)
      Then("the state is correct") {
        session.currentState() shouldBeEqual Inactive(id, "c1")
      }
    }
  }

  Given("a RegisterAndActivateCustomer command") {
    val command = RegisterAndActivateCustomer(id, "c1", reason = "cool")
    When("it's applied") {
      session.reset().decide(command)
      Then("the state is correct") {
        session.currentState() shouldBeEqual Active(id, "c1", reason = "cool")
      }
      Then("the events are correct") {
        session.appliedEvents() shouldBeEqual
          listOf(CustomerRegistered(id, "c1"), CustomerActivated("cool"))
      }
    }
  }

  Given("a RegisterCustomer command 1") {
    val command = RegisterCustomer(id, "c1")
    When("it's applied") {
      session.reset().decide(command)
      Then("the state is correct") {
        session.currentState() shouldBeEqual Inactive(id, "c1")
      }
      Then("the events are correct") {
        session.appliedEvents() shouldBeEqual
          listOf(CustomerRegistered(id, "c1"))
      }
    }
  }

  Given("a RegisterCustomer command 2") {
    val command1 = RegisterCustomer(id, "c1")
    And("a RenameCustomer command") {
      val command2 = RenameCustomer("c1-renamed")
      When("it's applied") {
        session.reset().decide(command1).decide(command2)
        Then("the state is correct") {
          session.currentState() shouldBeEqual Inactive(id, "c1-renamed")
        }
        Then("the events are correct") {
          session.appliedEvents() shouldBeEqual
            listOf(CustomerRegistered(id, "c1"), CustomerRenamed("c1-renamed"))
        }
      }
      When("it is renamed again") {
        session.decide(RenameCustomer("new name"))
        Then("the state is correct") {
          session.currentState() shouldBeEqual Inactive(id, "new name")
        }
      }
      Then("the events are correct") {
        session.appliedEvents() shouldBeEqual
          listOf(
            CustomerRegistered(id, "c1"),
            CustomerRenamed("c1-renamed"), CustomerRenamed("new name"),
          )
      }
    }
  }

  Given("a RegisterCustomer command 3") {
    val command1 = RegisterCustomer(id, "c1")
    When("it's applied") {
      session.reset().decide(command1)
      When("an invalid ActivateCustomer command is tried") {
        val forbiddenReason = "because I want it"
        val invalidCommand = ActivateCustomer(forbiddenReason)
        val exception =
          shouldThrow<Exception> {
            session.decide(invalidCommand)
          }
        Then("the exception is the expected") {
          exception.message?.shouldBeEqual("Reason cannot be = [$forbiddenReason], please be polite.")
        }
      }
    }
  }
})
