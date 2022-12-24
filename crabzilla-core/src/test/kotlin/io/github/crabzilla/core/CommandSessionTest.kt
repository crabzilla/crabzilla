package io.github.crabzilla.core

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.customerEventHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("A FeatureSession")
internal class CommandSessionTest {

  lateinit var commandSession: CommandSession<Customer, CustomerEvent>

  val customer = Customer.Initial

  @Test
  fun can_be_instantiated() {
    CommandSession(customer, customerEventHandler)
  }

  // TODO test
  //  val events = tracker
  //    .execute { c -> c.create(cmd.targetId, cmd.name) }
  //    .execute { c -> c.activate(cmd.reason) }
  //    .collectEvents()
  //

  @Nested
  @DisplayName("when new")
  internal inner class WhenIsNew {

    @BeforeEach
    fun instantiate() {
      commandSession = CommandSession(customer, customerEventHandler)
    }

    @Test
    fun is_empty() {
      assertThat(commandSession.appliedEvents.size).isEqualTo(0)
    }

    @Test
    fun has_empty_state() {
      assertThat(commandSession.currentState).isEqualTo(customer)
    }

    @Test
    fun statusData_matches() {
      val (newState, events, originalState) = commandSession.response()
      assertThat(newState).isEqualTo(customer)
      assertThat(originalState).isEqualTo(customer)
      assertThat(events).isEmpty()
    }

    @Nested
    @DisplayName("when adding a create customer event")
    internal inner class WhenAddingNewEvent {

      val id = UUID.randomUUID().toString()
      private val customerCreated = CustomerRegistered(id, "customer-1")
      private val expectedCustomer = Customer.Inactive(id, "customer-1")

      @BeforeEach
      fun apply_create_event() {
        commandSession.execute { customer -> listOf(customerCreated) }
      }

      @Test
      fun has_new_state() {
        assertThat(commandSession.currentState).isEqualTo(expectedCustomer)
      }

      @Test
      fun has_only_create_event() {
        assertThat(commandSession.appliedEvents).contains(customerCreated)
        assertThat(commandSession.appliedEvents.size).isEqualTo(1)
      }

      @Test
      fun statusData_matches() {
        val (originalState, events, newState) = commandSession.response()
        assertThat(newState).isEqualTo(expectedCustomer)
        assertThat(originalState).isEqualTo(customer)
        assertThat(events).containsOnly(customerCreated)
      }

      @Nested
      @DisplayName("when adding an activate customer event")
      internal inner class WhenAddingActivateEvent {

        private val customerActivated = CustomerActivated("is ok")
        private val expectedCustomer = Customer.Active(id, "customer-1", customerActivated.reason)

        @BeforeEach
        fun apply_activate_event() {
          commandSession.execute { customer -> listOf(customerActivated) }
        }

        @Test
        fun has_new_state() {
          assertThat(commandSession.currentState).isEqualTo(expectedCustomer)
        }

        @Test
        fun has_both_create_and_activated_evenst() {
          assertThat(commandSession.appliedEvents[0]).isEqualTo(customerCreated)
          assertThat(commandSession.appliedEvents[1]).isEqualTo(customerActivated)
          assertThat(commandSession.appliedEvents.size).isEqualTo(2)
        }
      }
    }
  }

  @Nested
  @DisplayName("when adding both create and activate events")
  internal inner class WhenAddingCreateActivateEvent {

    private val isOk = "is ok"
    private val id: String = UUID.randomUUID().toString()
    private val customerCreated = CustomerRegistered(id, "customer-1")
    private val customerActivated = CustomerActivated(isOk)
    private val expectedCustomer = Customer.Active(id, "customer-1", isOk)

    @BeforeEach
    fun instantiate() {
      // given
      commandSession = CommandSession(customer, customerEventHandler)
      // when
      commandSession.execute { listOf(customerCreated, customerActivated) }
    }

    // then

    @Test
    fun has_new_state() {
      assertThat(commandSession.currentState).isEqualTo(expectedCustomer)
    }

    @Test
    fun has_both_event() {
      assertThat(commandSession.appliedEvents).contains(customerCreated)
      assertThat(commandSession.appliedEvents).contains(customerActivated)
      assertThat(commandSession.appliedEvents.size).isEqualTo(2)
    }
  }

}
