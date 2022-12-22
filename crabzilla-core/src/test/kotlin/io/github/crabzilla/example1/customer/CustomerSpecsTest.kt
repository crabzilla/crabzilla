package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.CommandTestSpecification
import io.github.crabzilla.example1.customer.CustomerCommand.*
import io.github.crabzilla.example1.customer.CustomerEvent.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("Customer scenarios")
internal class CustomerSpecsTest {

  val id: String = UUID.randomUUID().toString()

  @Test
  fun `after a RegisterCustomer command, the state and events match`() {
    CommandTestSpecification(Customer.Initial, CustomerCommandHandler(), customerEventHandler)
      .whenCommand(RegisterCustomer(id, "c1"))
      .then {
        assertThat(it.state)
          .isEqualTo(Customer.Inactive(id, "c1"))
      }.then {
        assertThat(it.events)
          .isEqualTo(listOf(CustomerRegistered(id, "c1")))
      }
  }

  @Test
  fun `given a CustomerRegistered event then an ActivateCustomer, the state and events match`() {
    val spec = CommandTestSpecification(Customer.Inactive(id, "c1"), CustomerCommandHandler(),
      customerEventHandler)
      .givenEvents(CustomerRegistered(id, "c1"))
      .whenCommand(ActivateCustomer("bcoz yes"))
      .then {
        assertThat(it.state)
          .isEqualTo(Customer.Active(id, "c1", "bcoz yes"))
      }.then {
        assertThat(it.events)
          .isEqualTo(
            listOf(
              CustomerRegistered(id, "c1"),
              CustomerActivated("bcoz yes")
            )
          )
      }
    // when deactivated
    spec.whenCommand(DeactivateCustomer("bcoz bad customer"))
      .then {
        // then it's deactivated
        assertThat(spec.state)
          .isEqualTo(Customer.Inactive(id, "c1","bcoz bad customer"))
      }.then {
        // and it has these 3 events
        assertThat(spec.events)
          .isEqualTo(
            listOf(
              CustomerRegistered(id, "c1"),
              CustomerActivated("bcoz yes"),
              CustomerDeactivated("bcoz bad customer")
            )
          )
      }
  }

}
