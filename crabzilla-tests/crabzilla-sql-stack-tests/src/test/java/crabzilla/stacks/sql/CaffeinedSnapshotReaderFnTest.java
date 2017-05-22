package crabzilla.stacks.sql;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import crabzilla.Version;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.CustomerStateTransitionFnJavaslang;
import crabzilla.example1.aggregates.customer.CustomerSupplierFn;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.Event;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotData;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("A SnapshotReaderFn")
public class CaffeinedSnapshotReaderFnTest {

  Supplier<Customer> supplier ;
  Function<Customer, Customer> dependencyInjectionFn ;
  BiFunction<Event, Customer, Customer> stateTransitionFn ;
  Cache<String, Snapshot<Customer>> cache;

  @Mock
  EventRepository dao;

  @BeforeEach
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);
    supplier = new CustomerSupplierFn();
    dependencyInjectionFn  = customer -> customer;
    stateTransitionFn = new CustomerStateTransitionFnJavaslang();
    cache = Caffeine.newBuilder().build();
  }

  @Test
  public void on_empty_history_then_returns_version_0() throws ExecutionException {

    final CustomerId id = new CustomerId("customer#1");

    final Snapshot<Customer> expectedSnapshot =
            new Snapshot<>(supplier.get(), new Version(0L));

    final SnapshotData expectedHistory = new SnapshotData(new Version(0), Arrays.asList());

    when(dao.getAll(id.getStringValue())).thenReturn(expectedHistory);

    final CaffeinedSnapshotReaderFn<Customer> reader = new CaffeinedSnapshotReaderFn<>(cache, dao, supplier,
            dependencyInjectionFn, stateTransitionFn);

    assertThat(reader.getSnapshot(id.getStringValue())).isEqualTo(expectedSnapshot);

    verify(dao).getAll(id.getStringValue());

    verifyNoMoreInteractions(dao);

  }

  @Test
  public void on_empty_cache_then_returns_version_from_db() {

    final CustomerId id = new CustomerId("customer#1");
    final String name = "customer#1 name";

    final Customer expectedInstance = Customer.of(id, name, false, null);

    final Snapshot<Customer> expectedSnapshot =
            new Snapshot<>(expectedInstance, Version.create(1L));

    final CreateCustomerCmd command = new CreateCustomerCmd(UUID.randomUUID(), id, name);

    final SnapshotData expectedHistory =
            new SnapshotData(new Version(1), Arrays.asList(new CustomerCreated(id, command.getName())));

    when(dao.getAll(id.getStringValue())).thenReturn(expectedHistory);

    final CaffeinedSnapshotReaderFn<Customer> reader = new CaffeinedSnapshotReaderFn<>(cache, dao, supplier,
            dependencyInjectionFn, stateTransitionFn);

    assertThat(reader.getSnapshot(id.getStringValue())).isEqualTo(expectedSnapshot);

    verify(dao).getAll(id.getStringValue());

    verifyNoMoreInteractions(dao);


  }

  @Test
  public void on_cache_then_hits_db_to_check_newer_version() {

    final CustomerId id = new CustomerId("customer#1");
    final String name = "customer#1 name";

    final CreateCustomerCmd command = new CreateCustomerCmd(UUID.randomUUID(), id, name);

    final SnapshotData expectedHistory =
            new SnapshotData(new Version(1), Arrays.asList(new CustomerCreated(id, command.getName())));

    when(dao.getAll(id.getStringValue())).thenReturn(expectedHistory);

    verifyNoMoreInteractions(dao);

  }

  @Test
  public void on_both_cache_and_db_then_hits_db_to_compose_history() {

    val id = new CustomerId("customer#1");
    val name = "customer#1 name";
    val reason = "because yes";

    val cachedVersion = new Version(1L);
    val expectedVersion = new Version(2L);

    val cachedInstance = Customer.of(id, name, false, null);
    val expectedInstance = Customer.of(id, name, true, reason);

    val cachedSnapshot = new Snapshot<Customer>(cachedInstance, cachedVersion);
    val expectedSnapshot = new Snapshot<Customer>(expectedInstance, expectedVersion);

    final LocalDateTime activated_on = LocalDateTime.now();

    // cached history
    final CreateCustomerCmd command1 = new CreateCustomerCmd(UUID.randomUUID(), id, name);

    final SnapshotData cachedHistory =
            new SnapshotData(new Version(1), Arrays.asList(new CustomerCreated(id, command1.getName())));

    final SnapshotData nonCachedHistory =
            new SnapshotData(new Version(2), Arrays.asList(new CustomerActivated(reason, activated_on)));

    // prepare

    cache.put(id.getStringValue(), cachedSnapshot);

    when(dao.getAllAfterVersion(id.getStringValue(), cachedVersion)).thenReturn(nonCachedHistory);

    final CaffeinedSnapshotReaderFn<Customer> reader = new CaffeinedSnapshotReaderFn<>(cache, dao, supplier,
            dependencyInjectionFn, stateTransitionFn);

    Snapshot<Customer> resultingSnapshot = reader.getSnapshot(id.getStringValue());

    assertThat(expectedSnapshot).isEqualTo(resultingSnapshot);

    verify(dao).getAllAfterVersion(eq(id.getStringValue()), eq(cachedVersion));

    verifyNoMoreInteractions(dao);

  }

}

