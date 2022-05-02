package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.FeatureSpecification
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerDeactivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

@DisplayName("Customer scenarios")
internal class CustomerSpecsTest {

  val id: UUID = UUID.randomUUID()

  @Test
  fun `given a new customer after a command`() {
    FeatureSpecification(customerComponent)
      .whenCommand(RegisterCustomer(id, "c1"))
      .then {
        assertThat(it.state())
          .isEqualTo(Customer(id, "c1"))
      }.then {
        assertThat(it.events())
          .isEqualTo(listOf(CustomerRegistered(id, "c1")))
      }
  }

  @Test
  fun `given a registered customer given events`() {
    val spec = FeatureSpecification(customerComponent)
      .givenEvents(CustomerRegistered(id, "c1"))
      .whenCommand(ActivateCustomer("bcoz yes"))
      .then {
        assertThat(it.state())
          .isEqualTo(Customer(id, "c1", true, "bcoz yes"))
      }.then {
        assertThat(it.events())
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
        assertThat(spec.state())
          .isEqualTo(Customer(id, "c1", false, "bcoz bad customer"))
      }.then {
        // and it has these 3 events
        assertThat(spec.events())
          .isEqualTo(
            listOf(
              CustomerRegistered(id, "c1"),
              CustomerActivated("bcoz yes"),
              CustomerDeactivated("bcoz bad customer")
            )
          )
      }
  }

  @Test
  fun `trying to register a bad customer will fail`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
      .isThrownBy {
        FeatureSpecification(customerComponent)
          .whenCommand(RegisterCustomer(id, "bad customer"))
      }.withMessage("[Bad customer!]")
  }
}
