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
import java.util.UUID

@DisplayName("A StatefulSession")
internal class StatefulSessionTest {

  lateinit var statefulSession: StatefulSession<Customer, CustomerEvent>

  val customer = Customer(id = UUID.randomUUID(), name = "c1")

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
      statefulSession = StatefulSession(1, customer, customerEventHandler)
    }

    @Test
    fun is_empty() {
      assertThat(statefulSession.appliedEvents().size).isEqualTo(0)
    }

    @Test
    fun has_empty_state() {
      assertThat(statefulSession.currentState).isEqualTo(customer)
    }

    @Test
    fun statusData_matches() {
      val sessionData = statefulSession.toSessionData()
      assertThat(sessionData.newState).isEqualTo(customer)
      assertThat(sessionData.originalState).isEqualTo(customer)
      assertThat(sessionData.originalVersion).isEqualTo(1)
      assertThat(sessionData.events).isEmpty()
    }

    @Nested
    @DisplayName("when adding a create customer event")
    internal inner class WhenAddingNewEvent {

      val id = UUID.randomUUID()
      private val customerCreated = CustomerEvent.CustomerRegistered(id, "customer-1")
      private val expectedCustomer = Customer(id, "customer-1", false, null)

      @BeforeEach
      fun apply_create_event() {
        statefulSession.execute { customer -> listOf(customerCreated) }
      }

      @Test
      fun has_new_state() {
        assertThat(statefulSession.currentState).isEqualTo(expectedCustomer)
      }

      @Test
      fun has_only_create_event() {
        assertThat(statefulSession.appliedEvents()).contains(customerCreated)
        assertThat(statefulSession.appliedEvents().size).isEqualTo(1)
      }

      @Test
      fun statusData_matches() {
        val sessionData = statefulSession.toSessionData()
        assertThat(sessionData.newState).isEqualTo(expectedCustomer)
        assertThat(sessionData.originalState).isEqualTo(customer)
        assertThat(sessionData.originalVersion).isEqualTo(1)
        assertThat(sessionData.events).containsOnly(customerCreated)
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
          statefulSession.execute { customer -> listOf(customerActivated) }
        }

        @Test
        fun has_new_state() {
          assertThat(statefulSession.currentState).isEqualTo(expectedCustomer)
        }

        @Test
        fun has_both_create_and_activated_evenst() {
          assertThat(statefulSession.appliedEvents()[0]).isEqualTo(customerCreated)
          assertThat(statefulSession.appliedEvents()[1]).isEqualTo(customerActivated)
          assertThat(statefulSession.appliedEvents().size).isEqualTo(2)
        }
      }
    }
  }

  @Nested
  @DisplayName("when adding both create and activate events")
  internal inner class WhenAddingCreateActivateEvent {

    val isOk = "is ok"

    val id = UUID.randomUUID()
    private val customerCreated = CustomerEvent.CustomerRegistered(id, "customer-1")
    private val customerActivated = CustomerEvent.CustomerActivated(isOk)
    private val expectedCustomer = Customer(id, "customer-1", true, isOk)

    @BeforeEach
    fun instantiate() {
      // given
      statefulSession = StatefulSession(1, customer, customerEventHandler)
      // when
      statefulSession.execute { customer -> asList(customerCreated, customerActivated) }
    }

    // then

    @Test
    fun has_new_state() {
      assertThat(statefulSession.currentState).isEqualTo(expectedCustomer)
    }

    @Test
    fun has_both_event() {
      assertThat(statefulSession.appliedEvents()).contains(customerCreated)
      assertThat(statefulSession.appliedEvents()).contains(customerActivated)
      assertThat(statefulSession.appliedEvents().size).isEqualTo(2)
    }
  }
}
