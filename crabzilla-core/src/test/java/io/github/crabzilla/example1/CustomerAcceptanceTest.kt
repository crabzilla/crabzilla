package io.github.crabzilla.example1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CustomerAcceptanceTest {
  val state0 = Customer()
  val aware = CustomerCommandAware()
  @Nested
  inner class `Given a CustomerCreate event` {
    val customerId = 1
    val name = "Bob"
    val events1 = listOf(CustomerCreated(customerId, name))
    val state1 = events1.fold(state0) { state, event -> aware.applyEvent.invoke(event, state) }
    @Test
    fun `Then expect a Customer state is ok`() {
      assertThat(state1).isEqualTo(Customer(customerId = customerId, name = name))
    }
    @Nested
    inner class `When handling an ActivateCustomer command` {
      val reason = "because I need it"
      val command1 = ActivateCustomer(reason)
      val events2 = aware.handleCmd(customerId, state1, command1).result()
      val state2 = events1.plus(events2).fold(state1) { state, event -> aware.applyEvent.invoke(event, state) }
      @Test
      fun `Then a CustomerActivate event is generated`() {
        assertThat(events2).isEqualTo(listOf(CustomerActivated(reason)))
      }
      @Test
      fun `Then expect a Customer state is ok`() {
        assertThat(state2).isEqualTo(Customer(customerId, name, true, reason))
      }
    }
  }
}
