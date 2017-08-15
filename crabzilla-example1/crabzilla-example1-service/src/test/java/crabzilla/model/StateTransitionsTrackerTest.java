package crabzilla.model;

import crabzilla.example1.aggregates.Customer;
import crabzilla.example1.aggregates.CustomerData;
import crabzilla.example1.aggregates.CustomerFunctions;
import crabzilla.example1.aggregates.CustomerFunctionsVavr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A StateTransitionsTracker")
public class StateTransitionsTrackerTest {

  StateTransitionsTracker<Customer> tracker;

  Supplier<Customer> supplier = new CustomerFunctions.CustomerSupplierFn();

  @BeforeEach
  void instantiate() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void can_be_instantiated() {
    new StateTransitionsTracker<>(supplier.get(), new CustomerFunctionsVavr.CustomerStateTransitionFn(), customer -> customer);
  }

  @Nested
  @DisplayName("when new")
  public class WhenIsNew {

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(supplier.get(), new CustomerFunctionsVavr.CustomerStateTransitionFn(), customer -> customer);
    }

    @Test
    void is_empty() {
      assertThat(tracker.isEmpty()).isTrue();
    }

    @Test
    void has_empty_state() {
      assertThat(tracker.currentState()).isEqualTo(supplier.get());
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
      tracker = new StateTransitionsTracker<>(supplier.get(), new CustomerFunctionsVavr.CustomerStateTransitionFn(), customer -> customer);
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