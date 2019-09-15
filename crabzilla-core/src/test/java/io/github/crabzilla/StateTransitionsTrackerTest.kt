package io.github.crabzilla

import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommandAware
import io.github.crabzilla.framework.Snapshot
import io.github.crabzilla.framework.StateTransitionsTracker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Arrays.asList

@DisplayName("A StateTransitionsTracker")
internal class StateTransitionsTrackerTest {

  lateinit var tracker: StateTransitionsTracker<Customer>

  val customer = Customer()
  val originalSnapshot = Snapshot(customer, 0)

  @Test
  fun can_be_instantiated() {
    StateTransitionsTracker(originalSnapshot, CustomerCommandAware().applyEvent)
  }

  // TODO test
  //  val events = tracker
  //    .applyEvents { c -> c.create(cmd.targetId, cmd.name) }
  //    .applyEvents { c -> c.activate(cmd.reason) }
  //    .collectEvents()
  //

  @Nested
  @DisplayName("when new")
  internal inner class WhenIsNew {

    @BeforeEach
    fun instantiate() {
      tracker = StateTransitionsTracker(originalSnapshot, CustomerCommandAware().applyEvent)
    }

    @Test
    fun is_empty() {
      assertThat(tracker.appliedEvents.size).isEqualTo(0)
    }

    @Test
    fun has_empty_state() {
      assertThat(tracker.currentState).isEqualTo(originalSnapshot.state)
    }

    @Nested
    @DisplayName("when adding a create customer event")
    internal inner class WhenAddingNewEvent {

      val id: CustomerId = 1
      private val customerCreated = CustomerCreated(id, "customer-1")
      private val expectedCustomer = Customer(id, "customer-1", false, null)

      @BeforeEach
      fun apply_create_event() {
        tracker.applyEvents { (customerId, name, isActive, reason) -> listOf(customerCreated) }
      }

      @Test
      fun has_new_state() {
        assertThat(tracker.currentState).isEqualTo(expectedCustomer)
      }

      @Test
      fun has_only_create_event() {
        assertThat(tracker.appliedEvents).contains(customerCreated)
        assertThat(tracker.appliedEvents.size).isEqualTo(1)
      }

      @Nested
      @DisplayName("when adding an activate customer event")
      internal inner class WhenAddingActivateEvent {

        private val customerActivated = CustomerActivated("is ok", Instant.now())
        private val expectedCustomer = Customer(id, "customer-1", true,
                customerActivated.reason)

        @BeforeEach
        fun apply_activate_event() {
          tracker.applyEvents { (customerId, name, isActive, reason) -> listOf(customerActivated) }
        }

        @Test
        fun has_new_state() {
          assertThat(tracker.currentState).isEqualTo(expectedCustomer)
        }

        @Test
        fun has_both_create_and_activated_evenst() {
          assertThat(tracker.appliedEvents[0]).isEqualTo(customerCreated)
          assertThat(tracker.appliedEvents[1]).isEqualTo(customerActivated)
          assertThat(tracker.appliedEvents.size).isEqualTo(2)
        }

      }

    }

  }

  @Nested
  @DisplayName("when adding both create and activate events")
  internal inner class WhenAddingCreateActivateEvent {

    val isOk = "is ok"

    val id: CustomerId = 1
    private val customerCreated = CustomerCreated(id, "customer-1")
    private val customerActivated = CustomerActivated(isOk, Instant.now())
    private val expectedCustomer = Customer(id, "customer-1", true, isOk)

    @BeforeEach
    fun instantiate() {
      // given
      tracker = StateTransitionsTracker(originalSnapshot, CustomerCommandAware().applyEvent)
      // when
      tracker.applyEvents { (customerId, name, isActive, reason) -> asList(customerCreated, customerActivated) }
    }

    // then

    @Test
    fun has_new_state() {
      assertThat(tracker.currentState).isEqualTo(expectedCustomer)
    }

    @Test
    fun has_both_event() {
      assertThat(tracker.appliedEvents).contains(customerCreated)
      assertThat(tracker.appliedEvents).contains(customerActivated)
      assertThat(tracker.appliedEvents.size).isEqualTo(2)
    }

  }

}
