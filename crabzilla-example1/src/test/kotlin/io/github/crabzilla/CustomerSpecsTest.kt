package io.github.crabzilla

import io.github.crabzilla.core.CommandsSession
import io.github.crabzilla.core.TestSpecification
import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerCommand.RenameCustomer
import io.github.crabzilla.example1.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.CustomerEvent.CustomerRenamed
import io.github.crabzilla.example1.customerCommandHandler
import io.github.crabzilla.example1.customerEventHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("Customer scenarios : unit tests")
internal class CustomerSpecsTest {
  val id: String = UUID.randomUUID().toString()

  // TODO add test for exceptions

  @Test
  fun `given a CustomerRegistered event, the state is correct`() {
    val session = CommandsSession(Customer.Initial, customerEventHandler, customerCommandHandler)
    TestSpecification(session)
      .givenEvents(CustomerRegistered(id, "c1"))
      .then {
        assertThat(it.currentState()).isEqualTo(Customer.Inactive(id, "c1"))
      }
  }

  @Test
  fun `given a RegisterCustomer command, the state and events are correct`() {
    val session = CommandsSession(Customer.Initial, customerEventHandler, customerCommandHandler)
    TestSpecification(session)
      .whenCommand(RegisterCustomer(id, "c1"))
      .then {
        assertThat(it.appliedEvents()).isEqualTo(listOf(CustomerRegistered(id, "c1")))
      }.then {
        assertThat(it.currentState()).isEqualTo(Customer.Inactive(id, "c1"))
      }
  }

  @Test
  fun `given a RegisterCustomer and RenameCustomer commands, the state and events are correct`() {
    val session = CommandsSession(Customer.Initial, customerEventHandler, customerCommandHandler)
    val expectedEvents = listOf(CustomerRegistered(id, "c1"), CustomerRenamed("c1-renamed"))
    TestSpecification(session)
      .whenCommand(RegisterCustomer(id, "c1"))
      .whenCommand(RenameCustomer("c1-renamed"))
      .then {
        assertThat(it.appliedEvents()).isEqualTo(expectedEvents)
      }.then {
        assertThat(it.currentState()).isEqualTo(Customer.Inactive(id, "c1-renamed"))
      }
  }

  @Test
  fun `given a CustomerRegistered event then an ActivateCustomer, the state and events are correct`() {
    val session = CommandsSession(Customer.Inactive(id, "c1"), customerEventHandler, customerCommandHandler)
    TestSpecification(session)
      .givenEvents(CustomerRegistered(id, "c1"))
      .whenCommand(ActivateCustomer("bcoz yes"))
      .then {
        val expectedState = Customer.Active(id, "c1", "bcoz yes")
        assertThat(it.currentState()).isEqualTo(expectedState)
      }
      .then {
        val expectedEvents =
          listOf(
            CustomerRegistered(id, "c1"),
            CustomerActivated("bcoz yes"),
          )
        assertThat(it.appliedEvents()).isEqualTo(expectedEvents)
      }
      .whenCommand(DeactivateCustomer("bcoz bad customer"))
      .then {
        val expectedState = Customer.Inactive(id, "c1", "bcoz bad customer")
        assertThat(it.currentState()).isEqualTo(expectedState)
      }
      .then {
        val expectedEvents =
          listOf(
            CustomerRegistered(id, "c1"),
            CustomerActivated("bcoz yes"),
            CustomerDeactivated("bcoz bad customer"),
          )
        assertThat(it.appliedEvents()).isEqualTo(expectedEvents)
      }
  }
}
