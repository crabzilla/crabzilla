package io.github.crabzilla.core.command

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerEventHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Arrays.asList
import java.util.UUID

@DisplayName("A CommandSession")
internal class CommandSessionTest {

  lateinit var commandSession: CommandSession<Customer, CustomerEvent>

  val customer = Customer(id = UUID.randomUUID(), name = "c1")

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
      assertThat(commandSession.appliedEvents().size).isEqualTo(0)
    }

    @Test
    fun has_empty_state() {
      assertThat(commandSession.currentState).isEqualTo(customer)
    }

    @Test
    fun statusData_matches() {
      val sessionData = commandSession.toSessionData()
      assertThat(sessionData.newState).isEqualTo(customer)
      assertThat(sessionData.originalState).isEqualTo(customer)
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
        commandSession.execute { customer -> listOf(customerCreated) }
      }

      @Test
      fun has_new_state() {
        assertThat(commandSession.currentState).isEqualTo(expectedCustomer)
      }

      @Test
      fun has_only_create_event() {
        assertThat(commandSession.appliedEvents()).contains(customerCreated)
        assertThat(commandSession.appliedEvents().size).isEqualTo(1)
      }

      @Test
      fun statusData_matches() {
        val sessionData = commandSession.toSessionData()
        assertThat(sessionData.newState).isEqualTo(expectedCustomer)
        assertThat(sessionData.originalState).isEqualTo(customer)
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
          commandSession.execute { customer -> listOf(customerActivated) }
        }

        @Test
        fun has_new_state() {
          assertThat(commandSession.currentState).isEqualTo(expectedCustomer)
        }

        @Test
        fun has_both_create_and_activated_evenst() {
          assertThat(commandSession.appliedEvents()[0]).isEqualTo(customerCreated)
          assertThat(commandSession.appliedEvents()[1]).isEqualTo(customerActivated)
          assertThat(commandSession.appliedEvents().size).isEqualTo(2)
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
      commandSession = CommandSession(customer, customerEventHandler)
      // when
      commandSession.execute { customer -> asList(customerCreated, customerActivated) }
    }

    // then

    @Test
    fun has_new_state() {
      assertThat(commandSession.currentState).isEqualTo(expectedCustomer)
    }

    @Test
    fun has_both_event() {
      assertThat(commandSession.appliedEvents()).contains(customerCreated)
      assertThat(commandSession.appliedEvents()).contains(customerActivated)
      assertThat(commandSession.appliedEvents().size).isEqualTo(2)
    }
  }
}
