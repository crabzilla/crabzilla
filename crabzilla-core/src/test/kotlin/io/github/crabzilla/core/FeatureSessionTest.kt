package io.github.crabzilla.core

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand.UnknownCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.example1.customer.customerEventHandler
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Arrays.asList
import java.util.UUID

@DisplayName("A FeatureSession")
internal class FeatureSessionTest {

  lateinit var featureSession: FeatureSession<Customer, CustomerEvent>

  val customer = Customer(id = UUID.randomUUID(), name = "c1")

  @Test
  fun can_be_instantiated() {
    FeatureSession(customer, customerEventHandler)
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
      featureSession = FeatureSession(customer, customerEventHandler)
    }

    @Test
    fun is_empty() {
      assertThat(featureSession.appliedEvents().size).isEqualTo(0)
    }

    @Test
    fun has_empty_state() {
      assertThat(featureSession.currentState).isEqualTo(customer)
    }

    @Test
    fun statusData_matches() {
      val (newState, events, originalState) = featureSession.response()
      assertThat(newState).isEqualTo(customer)
      assertThat(originalState).isEqualTo(customer)
      assertThat(events).isEmpty()
    }

    @Nested
    @DisplayName("when adding a create customer event")
    internal inner class WhenAddingNewEvent {

      val id = UUID.randomUUID()
      private val customerCreated = CustomerEvent.CustomerRegistered(id, "customer-1")
      private val expectedCustomer = Customer(id, "customer-1", false, null)

      @BeforeEach
      fun apply_create_event() {
        featureSession.execute { customer -> listOf(customerCreated) }
      }

      @Test
      fun has_new_state() {
        assertThat(featureSession.currentState).isEqualTo(expectedCustomer)
      }

      @Test
      fun has_only_create_event() {
        assertThat(featureSession.appliedEvents()).contains(customerCreated)
        assertThat(featureSession.appliedEvents().size).isEqualTo(1)
      }

      @Test
      fun statusData_matches() {
        val (originalState, events, newState) = featureSession.response()
        assertThat(newState).isEqualTo(expectedCustomer)
        assertThat(originalState).isEqualTo(customer)
        assertThat(events).containsOnly(customerCreated)
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
          featureSession.execute { customer -> listOf(customerActivated) }
        }

        @Test
        fun has_new_state() {
          assertThat(featureSession.currentState).isEqualTo(expectedCustomer)
        }

        @Test
        fun has_both_create_and_activated_evenst() {
          assertThat(featureSession.appliedEvents()[0]).isEqualTo(customerCreated)
          assertThat(featureSession.appliedEvents()[1]).isEqualTo(customerActivated)
          assertThat(featureSession.appliedEvents().size).isEqualTo(2)
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
      featureSession = FeatureSession(customer, customerEventHandler)
      // when
      featureSession.execute { customer -> asList(customerCreated, customerActivated) }
    }

    // then

    @Test
    fun has_new_state() {
      assertThat(featureSession.currentState).isEqualTo(expectedCustomer)
    }

    @Test
    fun has_both_event() {
      assertThat(featureSession.appliedEvents()).contains(customerCreated)
      assertThat(featureSession.appliedEvents()).contains(customerActivated)
      assertThat(featureSession.appliedEvents().size).isEqualTo(2)
    }
  }

  @Test
  fun `a UnknownCommand will fail`() {

    Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java)
      .isThrownBy {
        FeatureSpecification(customerComponent)
          .whenCommand(UnknownCommand("?"))
      }.withMessage(UnknownCommand::class.java.canonicalName)
  }
}
