package crabzilla.stack;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A StateTransitionsTracker")
public class StateTransitionsTrackerTest {

  final Injector injector = Guice.createInjector(new CustomerModule());

  @Inject
  Supplier<Customer> supplier;
  @Inject
  Function<Customer, Customer> dependencyInjectionFn;
  @Inject
  BiFunction<Event, Customer, Customer> stateTransitionFn;

  StateTransitionsTracker<Customer> tracker;

  @BeforeEach
  void instantiate() {
    injector.injectMembers(this);
  }

  @Test
  public void can_be_instantiated() {
    injector.injectMembers(this);
    new StateTransitionsTracker<>(supplier.get(), stateTransitionFn, dependencyInjectionFn);
  }

  @Nested
  @DisplayName("when new")
  public class WhenIsNew {

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(supplier.get(), stateTransitionFn, dependencyInjectionFn);
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

      final CustomerId id = new CustomerId("c1");
      private CustomerCreated createdEvent = new CustomerCreated(id, "customer-1");
      private Customer expectedCustomer = Customer.of(id, "customer-1", false, null);

      @BeforeEach
      void apply_create_event() {
        tracker.applyEvents(Arrays.asList(createdEvent));
      }

      @Test
      void has_new_state() {
        assertThat(tracker.currentState()).isEqualTo(expectedCustomer);
      }

      @Test
      void has_only_create_event() {
        assertThat(tracker.getEvents()).contains(createdEvent);
        assertThat(tracker.getEvents().size()).isEqualTo(1);
      }

      @Nested
      @DisplayName("when adding an activate customer event")
      public class WhenAddingActivateEvent {

        private CustomerActivated customerActivated = new CustomerActivated("is ok", LocalDateTime.now());
        private Customer expectedCustomer = Customer.of(id, "customer-1", true,
                customerActivated.getReason());

        @BeforeEach
        void apply_activate_event() {
          tracker.applyEvents(Arrays.asList(customerActivated));
        }

        @Test
        void has_new_state() {
          assertThat(tracker.currentState()).isEqualTo(expectedCustomer);
        }

        @Test
        void has_both_create_and_activated_evenst() {
          assertThat(tracker.getEvents().get(0)).isEqualTo(createdEvent);
          assertThat(tracker.getEvents().get(1)).isEqualTo(customerActivated);
          assertThat(tracker.getEvents().size()).isEqualTo(2);
        }

      }

    }

  }

}