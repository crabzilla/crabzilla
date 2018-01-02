package io.github.crabzilla.core.entity;

import io.github.crabzilla.example1.SampleInternalService;
import io.github.crabzilla.example1.customer.Customer;
import io.github.crabzilla.example1.customer.CustomerCreated;
import io.github.crabzilla.example1.customer.CustomerId;
import io.github.crabzilla.example1.customer.StateTransitionFn;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("A SnapshotPromoter")
public class SnapshotPromoterTest {

  SnapshotPromoter<Customer> promoter;
  StateTransitionsTracker<Customer> tracker;

  @Mock
  Function1<Snapshot<? extends Customer>, StateTransitionsTracker<Customer>> factory;

  final SampleInternalService service = new TestSampleInternalService();
  final Customer customer = new Customer(null, null, false, null, service);
  final Snapshot<Customer> originalSnapshot = new Snapshot<>(customer, new Version(0));

  @BeforeEach
  void instantiate() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void can_be_instantiated() {
    new SnapshotPromoter<Customer>(factory);
  }

  @Nested
  @DisplayName("When promoting an empty snapshot with single event single event to version 1")
  public class WhenPromotingAnEmptyToV1 {

    final CustomerId id = new CustomerId("c1");
    final CustomerCreated customerCreated = new CustomerCreated(id, "customer-1");
    Snapshot<Customer> resultingSnapshot1;

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(originalSnapshot, new StateTransitionFn());
      when(factory.invoke(eq(originalSnapshot))).thenReturn(tracker);
      promoter = new SnapshotPromoter<Customer>(factory);
    }

    @Test
    void it_has_expected_snapshot() {
      resultingSnapshot1 = promoter.promote(originalSnapshot, new Version(1), asList(customerCreated));
      final Customer expectedCustomer1 = new Customer(id, "customer-1", false, null, service);
      final Snapshot<Customer> expectedSnapshot1 = new Snapshot<>(expectedCustomer1, new Version(1));
      assertThat(resultingSnapshot1).isEqualTo(expectedSnapshot1);
    }

  }

  @Nested
  @DisplayName("When promoting an empty snapshot with single event single event to version 2")
  public class WhenPromotingAnEmptyToV2 {

    final CustomerId id = new CustomerId("c1");
    final CustomerCreated customerCreated = new CustomerCreated(id, "customer-1");
    Snapshot<Customer> resultingSnapshot1;

    @BeforeEach
    void instantiate() {
      tracker = new StateTransitionsTracker<>(originalSnapshot, new StateTransitionFn());
      when(factory.invoke(eq(originalSnapshot))).thenReturn(tracker);
      promoter = new SnapshotPromoter<Customer>(factory);
    }

    @Test
    void it_should_throw_an_exception() {
      Throwable exception = assertThrows(RuntimeException.class, () ->
      {
        resultingSnapshot1 = promoter.promote(originalSnapshot, new Version(2), asList(customerCreated));
      });
      assertEquals("Cannot upgrade to version Version(valueAsLong=2) since current version is Version(valueAsLong=0)",
              exception.getMessage());
    }

  }

}