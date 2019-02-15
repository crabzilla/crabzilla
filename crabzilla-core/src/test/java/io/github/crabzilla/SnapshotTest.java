package io.github.crabzilla;

import io.github.crabzilla.example1.Customer;
import io.github.crabzilla.example1.CustomerCreated;
import io.github.crabzilla.example1.CustomerId;
import io.github.crabzilla.example1.PojoService;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static io.github.crabzilla.example1.CustomerKt.getCUSTOMER_STATE_BUILDER;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("A Snapshot")
class SnapshotTest {

  StateTransitionsTracker<Customer> tracker;

  @Mock
  Function1<Snapshot<Customer>, StateTransitionsTracker<Customer>> factory;

  final PojoService service = new PojoService();
  final Customer customer = new Customer(null, null, false, null, service);
  final Snapshot<Customer> originalSnapshot = new Snapshot<>(customer, 0);

  @BeforeEach
  void instantiate() {
    MockitoAnnotations.initMocks(this);
  }

  @Nested
  @DisplayName("when upgrading an empty snapshot with single event single event to version 1")
  class a1 {

    CustomerId id = new CustomerId(1);
    CustomerCreated customerCreated = new CustomerCreated(id, "customer-1");
    Snapshot<Customer> resultingSnapshot1;

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(originalSnapshot, getCUSTOMER_STATE_BUILDER());
      when(factory.invoke(eq(originalSnapshot))).thenReturn(tracker);
    }

    @Test
    void it_has_expected_snapshot() {
      resultingSnapshot1 = originalSnapshot.upgradeTo(1, singletonList(customerCreated), getCUSTOMER_STATE_BUILDER());
      final Customer expectedCustomer1 = new Customer(id, "customer-1", false, null, service);
      final Snapshot<Customer> expectedSnapshot1 = new Snapshot<>(expectedCustomer1, 1);
      assertThat(resultingSnapshot1).isEqualTo(expectedSnapshot1);
    }

  }

  @Nested
  @DisplayName("when upgrading an empty snapshot with single event single event to version 2")
  class a2 {

    final CustomerId id = new CustomerId(1);
    final CustomerCreated customerCreated = new CustomerCreated(id, "customer-1");
    Snapshot<Customer> resultingSnapshot1;

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(originalSnapshot, getCUSTOMER_STATE_BUILDER());
      when(factory.invoke(eq(originalSnapshot))).thenReturn(tracker);
    }

    @Test
    void it_should_throw_an_exception() {
      Throwable exception = assertThrows(RuntimeException.class, () ->
        resultingSnapshot1 = originalSnapshot.upgradeTo(2, singletonList(customerCreated), getCUSTOMER_STATE_BUILDER()));
      assertEquals("Cannot upgrade to version 2 since current version is 0",
              exception.getMessage());
    }

  }

}
