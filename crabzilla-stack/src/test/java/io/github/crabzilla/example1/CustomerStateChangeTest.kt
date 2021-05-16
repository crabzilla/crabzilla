package io.github.crabzilla.example1

import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.example1.CustomerCommandHandler.handleCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Not an exhaustive test. Just as an example on how to test a domain.
 */
class CustomerStateChangeTest {

  @Nested
  inner class `Given a new Customer` {
    val customerId = UUID.randomUUID()
    val name = "Bob"
    val result = Customer.create(id = customerId, name = name)

    @Test
    fun `Then Customer is created`() {
      assertThat(result.state).isEqualTo(Customer(id = customerId, name = name))
      assertThat(result.events.first()).isEqualTo(CustomerEvent.CustomerRegistered(id = customerId, name = name))
    }

    @Nested
    inner class `Given a CustomerActivate event` {
      val reason = "because I need it"
      val event2 = CustomerEvent.CustomerActivated(reason)
      val state2 = customerEventHandler.handleEvent(result.state, event2)

      @Test
      fun `Then Customer is activated`() {
        assertThat(state2).isEqualTo(Customer(id = customerId, name = name, reason = reason, isActive = true))
      }

      @Nested
      inner class `Given a CustomerDeactivate event` {
        val reason = "because I need it again"
        val event3 = CustomerEvent.CustomerDeactivated(reason)
        val state3 = customerEventHandler.handleEvent(state2, event3)

        @Test
        fun `Then Customer is deactivated`() {
          assertThat(state3).isEqualTo(Customer(id = customerId, name = name, reason = reason, isActive = false))
        }

        @Nested
        inner class `Given a CustomerActivate command` {
          val reason = "because I need it again and again"
          val cmd = CustomerCommand.ActivateCustomer(reason)
          val result = handleCommand(cmd, Snapshot(state3, 1))

          @Test
          fun `Then Customer is activated`() {
            assertThat(result!!.currentState)
              .isEqualTo(Customer(id = customerId, name = name, reason = reason, isActive = true))
          }

          @Test
          fun `Then the event is CustomerActivated`() {
            assertThat(result!!.appliedEvents().first())
              .isEqualTo(CustomerEvent.CustomerActivated(reason = reason))
          }

          @Test
          fun `Then the original version is 1`() {
            assertThat(result!!.originalVersion)
              .isEqualTo(1)
          }
        }
      }
    }
  }
}
