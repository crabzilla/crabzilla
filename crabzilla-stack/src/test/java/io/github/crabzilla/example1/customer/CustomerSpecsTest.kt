package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.CommandException.ValidationException
import io.github.crabzilla.core.TestSpecification
import io.github.crabzilla.example1.customer.CustomerCommand.*
import io.github.crabzilla.example1.customer.CustomerEvent.*
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
          CustomerDeactivated("bcoz bad customer")
        )
      )
  }

  @Test
  fun `trying to register a bad customer will fail`() {

    assertThatExceptionOfType(ValidationException::class.java)
      .isThrownBy {
        TestSpecification(customerConfig)
          .whenCommand(RegisterCustomer(id, "bad customer"))
      }.withMessage("[Bad customer!]")
  }

}
