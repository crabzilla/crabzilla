package io.github.crabzilla.core

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerEventHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Arrays.asList

@DisplayName("A StatefulSession")
internal class StatefulSessionTest {

  lateinit var tracker: StatefulSession<Customer, CustomerEvent>

  val customer = Customer(id = 1, name = "c1")

  @Test
  fun can_be_instantiated() {
    StatefulSession(1, customer, customerEventHandler)
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
      tracker = StatefulSession(1, customer, customerEventHandler)
    }

    @Test
    fun is_empty() {
      assertThat(tracker.appliedEvents().size).isEqualTo(0)
    }

    @Test
    fun has_empty_state() {
      assertThat(tracker.currentState).isEqualTo(customer)
    }

    @Nested
    @DisplayName("when adding a create customer event")
    internal inner class WhenAddingNewEvent {

      val id = 1
      private val customerCreated = CustomerEvent.CustomerRegistered(id, "customer-1")
      private val expectedCustomer = Customer(id, "customer-1", false, null)

      @BeforeEach
      fun apply_create_event() {
        tracker.execute { customer -> listOf(customerCreated) }
      }

      @Test
      fun has_new_state() {
        assertThat(tracker.currentState).isEqualTo(expectedCustomer)
      }

      @Test
      fun has_only_create_event() {
        assertThat(tracker.appliedEvents()).contains(customerCreated)
        assertThat(tracker.appliedEvents().size).isEqualTo(1)
      }

      @Nested
      @DisplayName("when adding an activate customer event")
      internal inner class WhenAddingActivateEvent {

        private val customerActivated = CustomerEvent.CustomerActivated("is ok")
        private val expectedCustomer = Customer(
          id, "customer-1", true,
          customerActivated.reason
        )

        @BeforeEach
        fun apply_activate_event() {
          tracker.execute { customer -> listOf(customerActivated) }
        }

        @Test
        fun has_new_state() {
          assertThat(tracker.currentState).isEqualTo(expectedCustomer)
        }

        @Test
        fun has_both_create_and_activated_evenst() {
          assertThat(tracker.appliedEvents()[0]).isEqualTo(customerCreated)
          assertThat(tracker.appliedEvents()[1]).isEqualTo(customerActivated)
          assertThat(tracker.appliedEvents().size).isEqualTo(2)
        }
      }
    }
  }

  @Nested
  @DisplayName("when adding both create and activate events")
  internal inner class WhenAddingCreateActivateEvent {

    val isOk = "is ok"

    val id = 1
    private val customerCreated = CustomerEvent.CustomerRegistered(id, "customer-1")
    private val customerActivated = CustomerEvent.CustomerActivated(isOk)
    private val expectedCustomer = Customer(id, "customer-1", true, isOk)

    @BeforeEach
    fun instantiate() {
      // given
      tracker = StatefulSession(1, customer, customerEventHandler)
      // when
      tracker.execute { customer -> asList(customerCreated, customerActivated) }
    }

    // then

    @Test
    fun has_new_state() {
      assertThat(tracker.currentState).isEqualTo(expectedCustomer)
    }

    @Test
    fun has_both_event() {
      assertThat(tracker.appliedEvents()).contains(customerCreated)
      assertThat(tracker.appliedEvents()).contains(customerActivated)
      assertThat(tracker.appliedEvents().size).isEqualTo(2)
    }
  }
}
