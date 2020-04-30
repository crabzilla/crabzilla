package io.github.crabzilla.example1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CustomerAcceptanceTests {
  val state0 = Customer()
  val aware = CustomerCommandAware()
  @Nested
  inner class `Given events` {
    val events1 = listOf(CustomerCreated(1, "Bob"))
    val state1 = events1.fold(state0) { state, event -> aware.applyEvent.invoke(event, state) }
    @Nested
    inner class `When activating it` {
      val command = ActivateCustomer("because I need it")
      val events2 = aware.handleCmd(1, state1, command).result()
      val state2 = events1.plus(events2).fold(state1) { state, event -> aware.applyEvent.invoke(event, state) }
      @Test
      fun `Then id = 1`() {
        assertThat(state2.customerId).isEqualTo(1)
      }
      @Test
      fun `Then name = Bob`() {
        assertThat(state2.name).isEqualTo("Bob")
      }
      @Test
      fun `Then isActive is true`() {
        assertThat(state2.isActive).isTrue()
      }
    }
  }
}
