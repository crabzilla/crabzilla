package crabzilla.model;

import crabzilla.example1.customer.Customer;
import crabzilla.example1.customer.CustomerData;
import crabzilla.example1.customer.CustomerFunctionsVavr;
import crabzilla.example1.services.SampleInternalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A StateTransitionsTracker")
public class StateTransitionsTrackerTest {

  StateTransitionsTracker<Customer> tracker;

  final Customer customer = new Customer(null, null, null, false, null)
            .withService(new SampleInternalServiceImpl());

  final Snapshot<Customer> originalSnapshot = new Snapshot<>(customer, new Version(0));

  @BeforeEach
  void instantiate() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void can_be_instantiated() {
    new StateTransitionsTracker<>(originalSnapshot, new CustomerFunctionsVavr.StateTransitionFn());
  }

  @Nested
  @DisplayName("when new")
  public class WhenIsNew {

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(originalSnapshot,
              new CustomerFunctionsVavr.StateTransitionFn());
    }

    @Test
    void is_empty() {
      assertThat(tracker.isEmpty()).isTrue();
    }

    @Test
    void has_empty_state() {
      assertThat(tracker.currentState()).isEqualTo(originalSnapshot.getInstance());
    }

    @Nested
    @DisplayName("when adding a create customer event")
    public class WhenAddingNewEvent {

      final CustomerData.CustomerId id = new CustomerData.CustomerId("c1");
      private CustomerData.CustomerCreated customerCreated = new CustomerData.CustomerCreated(id, "customer-1");
      private Customer expectedCustomer = Customer.of(id, "customer-1", false, null);

      @BeforeEach
      void apply_create_event() {
        tracker.applyEvents(c -> singletonList(customerCreated));
      }

      @Test
      void has_new_state() {
        assertThat(tracker.currentState()).isEqualTo(expectedCustomer);
      }

      @Test
      void has_only_create_event() {
        assertThat(tracker.collectEvents()).contains(customerCreated);
        assertThat(tracker.collectEvents().size()).isEqualTo(1);
      }

      @Nested
      @DisplayName("when adding an activate customer event")
      public class WhenAddingActivateEvent {

        private CustomerData.CustomerActivated customerActivated = new CustomerData.CustomerActivated("is ok", Instant.now());
        private Customer expectedCustomer = Customer.of(id, "customer-1", true,
                customerActivated.getReason());

        @BeforeEach
        void apply_activate_event() {
          tracker.applyEvents(c -> singletonList(customerActivated));
        }

        @Test
        void has_new_state() {
          assertThat(tracker.currentState()).isEqualTo(expectedCustomer);
        }

        @Test
        void has_both_create_and_activated_evenst() {
          assertThat(tracker.collectEvents().get(0)).isEqualTo(customerCreated);
          assertThat(tracker.collectEvents().get(1)).isEqualTo(customerActivated);
          assertThat(tracker.collectEvents().size()).isEqualTo(2);
        }

      }

    }

  }

  @Nested
  @DisplayName("when adding both create and activate events")
  public class WhenAddingCreateActivateEvent {

    final String IS_OK = "is ok";

    final CustomerData.CustomerId id = new CustomerData.CustomerId("c1");
    private CustomerData.CustomerCreated customerCreated = new CustomerData.CustomerCreated(id, "customer-1");
    private CustomerData.CustomerActivated customerActivated = new CustomerData.CustomerActivated(IS_OK, Instant.now());
    private Customer expectedCustomer = Customer.of(id, "customer-1", true, IS_OK);

    @BeforeEach
    void instantiate() {
      // given
      tracker = new StateTransitionsTracker<>(originalSnapshot, new CustomerFunctionsVavr.StateTransitionFn());
      // when
      tracker.applyEvents(c -> asList(customerCreated, customerActivated));
    }

    // then

    @Test
    void has_new_state() {
      assertThat(tracker.currentState()).isEqualTo(expectedCustomer);
    }

    @Test
    void has_both_event() {
      assertThat(tracker.collectEvents()).contains(customerCreated);
      assertThat(tracker.collectEvents()).contains(customerActivated);
      assertThat(tracker.collectEvents().size()).isEqualTo(2);
    }

  }

}