package io.github.crabzilla;

import io.github.crabzilla.example1.Customer;
import io.github.crabzilla.example1.CustomerActivated;
import io.github.crabzilla.example1.CustomerCreated;
import io.github.crabzilla.example1.CustomerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static io.github.crabzilla.example1.CustomerKt.getCUSTOMER_STATE_BUILDER;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A StateTransitionsTracker")
class StateTransitionsTrackerTest {

  StateTransitionsTracker<Customer> tracker;

  final Customer customer = new Customer();
  final Snapshot<Customer> originalSnapshot = new Snapshot<>(customer, 0);

  @BeforeEach
  void instantiate() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void can_be_instantiated() {
    new StateTransitionsTracker<>(originalSnapshot, getCUSTOMER_STATE_BUILDER());
  }

  // TODO test
  //  val events = tracker
  //    .applyEvents { c -> c.create(cmd.targetId, cmd.name) }
  //    .applyEvents { c -> c.activate(cmd.reason) }
  //    .collectEvents()
  //

  @Nested
  @DisplayName("when new")
  class WhenIsNew {

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(originalSnapshot, getCUSTOMER_STATE_BUILDER());
    }

    @Test
    void is_empty() {
      assertThat(tracker.getAppliedEvents().size()).isEqualTo(0);
    }

    @Test
    void has_empty_state() {
      assertThat(tracker.getCurrentState()).isEqualTo(originalSnapshot.getState());
    }

    @Nested
    @DisplayName("when adding a create customer event")
    class WhenAddingNewEvent {

      final CustomerId id = new CustomerId(1);
      private CustomerCreated customerCreated = new CustomerCreated(id, "customer-1");
      private Customer expectedCustomer = new Customer(id, "customer-1", false, null);

      @BeforeEach
      void apply_create_event() {
        tracker.applyEvents(c -> singletonList(customerCreated));
      }

      @Test
      void has_new_state() {
        assertThat(tracker.getCurrentState()).isEqualTo(expectedCustomer);
      }

      @Test
      void has_only_create_event() {
        assertThat(tracker.getAppliedEvents()).contains(customerCreated);
        assertThat(tracker.getAppliedEvents().size()).isEqualTo(1);
      }

      @Nested
      @DisplayName("when adding an activate customer event")
      class WhenAddingActivateEvent {

        private CustomerActivated customerActivated = new CustomerActivated("is ok", Instant.now());
        private Customer expectedCustomer = new Customer(id, "customer-1", true,
                customerActivated.getReason());

        @BeforeEach
        void apply_activate_event() {
          tracker.applyEvents(c -> singletonList(customerActivated));
        }

        @Test
        void has_new_state() {
          assertThat(tracker.getCurrentState()).isEqualTo(expectedCustomer);
        }

        @Test
        void has_both_create_and_activated_evenst() {
          assertThat(tracker.getAppliedEvents().get(0)).isEqualTo(customerCreated);
          assertThat(tracker.getAppliedEvents().get(1)).isEqualTo(customerActivated);
          assertThat(tracker.getAppliedEvents().size()).isEqualTo(2);
        }

      }

    }

  }

  @Nested
  @DisplayName("when adding both create and activate events")
  class WhenAddingCreateActivateEvent {

    final String IS_OK = "is ok";

    final CustomerId id = new CustomerId(1);
    private CustomerCreated customerCreated = new CustomerCreated(id, "customer-1");
    private CustomerActivated customerActivated = new CustomerActivated(IS_OK, Instant.now());
    private Customer expectedCustomer = new Customer(id, "customer-1", true, IS_OK);

    @BeforeEach
    void instantiate() {
      // given
      tracker = new StateTransitionsTracker<>(originalSnapshot, getCUSTOMER_STATE_BUILDER());
      // when
      tracker.applyEvents(c -> asList(customerCreated, customerActivated));
    }

    // then

    @Test
    void has_new_state() {
      assertThat(tracker.getCurrentState()).isEqualTo(expectedCustomer);
    }

    @Test
    void has_both_event() {
      assertThat(tracker.getAppliedEvents()).contains(customerCreated);
      assertThat(tracker.getAppliedEvents()).contains(customerActivated);
      assertThat(tracker.getAppliedEvents().size()).isEqualTo(2);
    }

  }

}
