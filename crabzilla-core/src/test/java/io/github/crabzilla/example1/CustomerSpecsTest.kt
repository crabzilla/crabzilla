package io.github.crabzilla.example1

import io.github.crabzilla.core.CommandException.ValidationException
import io.github.crabzilla.core.TestSpecification
import io.github.crabzilla.example1.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.CustomerEvent.CustomerRegistered
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import java.util.UUID

class CustomerSpecsTest {

  val id = UUID.randomUUID()

  @Test
  fun `given a new customer`() {

    val spec = TestSpecification(customerConfig)
      .whenCommand(RegisterCustomer(id, "c1"))

    assertThat(spec.state())
      .isEqualTo(Customer(id, "c1"))

    assertThat(spec.events())
      .isEqualTo(listOf(CustomerRegistered(id, "c1")))
  }

  @Test
  fun `trying to register a bad customer will fail`() {

    assertThatExceptionOfType(ValidationException::class.java)
      .isThrownBy {
        TestSpecification(customerConfig)
          .whenCommand(RegisterCustomer(id, "bad customer"))
      }.withMessage("[Bad customer!]")
  }

  @Test
  fun `given a registered customer`() {

    // give an existing customer
    val spec = TestSpecification(customerConfig)
      .givenEvents(CustomerRegistered(id, "c1"))

    // when activated
    spec.whenCommand(ActivateCustomer("bcoz yes"))

    // then it is activated
    assertThat(spec.state())
      .isEqualTo(Customer(id, "c1", true, "bcoz yes"))

    // an it has these 2 events
    assertThat(spec.events())
      .isEqualTo(
        listOf(
          CustomerRegistered(id, "c1"),
          CustomerActivated("bcoz yes")
        )
      )

    // when deactivated
    spec.whenCommand(DeactivateCustomer("bcoz bad customer"))

    // then it's deactivated
    assertThat(spec.state())
      .isEqualTo(Customer(id, "c1", false, "bcoz bad customer"))

    // an it has these 3 events
    assertThat(spec.events())
      .isEqualTo(
        listOf(
          CustomerRegistered(id, "c1"),
          CustomerActivated("bcoz yes"),
          CustomerEvent.CustomerDeactivated("bcoz bad customer")
        )
      )
  }
}
