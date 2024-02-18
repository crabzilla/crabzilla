package io.crabzilla.example1.customer

import io.crabzilla.core.CrabzillaCommandsSession
import io.crabzilla.core.TestSpecification
import io.crabzilla.example1.customer.model.Customer
import io.crabzilla.example1.customer.model.CustomerCommand
import io.crabzilla.example1.customer.model.CustomerCommand.ActivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.RegisterAndActivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.RegisterCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.RenameCustomer
import io.crabzilla.example1.customer.model.CustomerEvent
import io.crabzilla.example1.customer.model.CustomerEvent.CustomerActivated
import io.crabzilla.example1.customer.model.CustomerEvent.CustomerDeactivated
import io.crabzilla.example1.customer.model.CustomerEvent.CustomerRegistered
import io.crabzilla.example1.customer.model.CustomerEvent.CustomerRenamed
import io.crabzilla.example1.customer.model.customerDeciderFunction
import io.crabzilla.example1.customer.model.customerEvolveFunction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("Customer scenarios")
class CustomerSpecsTest {
  private val id = UUID.randomUUID()
  private lateinit var session: CrabzillaCommandsSession<CustomerCommand, Customer, CustomerEvent>

  @BeforeEach
  fun setup() {
    session = CrabzillaCommandsSession(Customer.Initial, customerEvolveFunction, customerDeciderFunction)
  }

  @Test
  fun `given a CustomerRegistered event, the state is correct`() {
    TestSpecification(session)
      .givenEvents(CustomerRegistered(id, "c1"))
      .then {
        assertThat(it.currentState()).isEqualTo(Customer.Inactive(id, "c1"))
      }
  }

  @Test
  fun `given a RegisterAndActivateCustomer command, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterAndActivateCustomer(id, "c1", reason = "cool"))
      .then {
        val expectedState = Customer.Active(id, "c1", reason = "cool")
        assertThat(it.currentState()).isEqualTo(expectedState)
      }
      .then {
        val expectedEvents = listOf(CustomerRegistered(id, "c1"), CustomerActivated("cool"))
        assertThat(it.appliedEvents()).isEqualTo(expectedEvents)
      }
  }

  @Test
  fun `given a RegisterCustomer command, the state and events are correct`() {
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

  @Test
  fun `given a error, it will be caught`() {
    val forbiddenReason = "because I want it"
    TestSpecification(session)
      .givenEvents(CustomerRegistered(id, "c1"))
      .whenCommand(ActivateCustomer(forbiddenReason))
      .then {
        assertThat(it.lastException()!!.message).isEqualTo("Reason cannot be = [$forbiddenReason], please be polite.")
      }
  }
}
