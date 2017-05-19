package crabzilla.stack1;

import com.google.inject.Guice;
import com.google.inject.Injector;
import crabzilla.Version;
import crabzilla.example1.aggregates.CustomerModule;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.Event;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotData;
import org.apache.camel.com.github.benmanes.caffeine.cache.Cache;
import org.apache.camel.com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("A SnapshotReader")
public class Stack1SnapshotReaderTest {

  final Injector injector = Guice.createInjector(new CustomerModule());

  @Inject
  Supplier<Customer> supplier;
  @Inject
  Function<Customer, Customer> dependencyInjectionFn;
  @Inject
  BiFunction<Event, Customer, Customer> stateTransitionFn;

  @Mock
  EventRepository dao;

  Cache<CustomerId, SnapshotData> cache;

  @BeforeEach
  public void init() throws Exception {
    cache = Caffeine.newBuilder().build();
    injector.injectMembers(this);
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void on_empty_history_then_returns_version_0() throws ExecutionException {

    final CustomerId id = new CustomerId("customer#1");

    final Snapshot<Customer> expectedSnapshot =
            new Snapshot<>(supplier.get(), new Version(0L));

    final SnapshotData expectedHistory = new SnapshotData(new Version(0), Arrays.asList());

    when(dao.getAll(id.getStringValue())).thenReturn(expectedHistory);

    final Stack1SnapshotReader<CustomerId, Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
            dependencyInjectionFn, stateTransitionFn);

    assertThat(reader.getSnapshot(id)).isEqualTo(expectedSnapshot);

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

    final Stack1SnapshotReader<CustomerId, Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
            dependencyInjectionFn, stateTransitionFn);

    assertThat(reader.getSnapshot(id)).isEqualTo(expectedSnapshot);

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

    cache.put(id, expectedHistory);

    verifyNoMoreInteractions(dao);

  }

  @Test
  public void on_both_cache_and_db_then_hits_db_to_compose_history() {

    final CustomerId id = new CustomerId("customer#1");
    final String name = "customer#1 name";
    final String reason = "because yes";

    final Version cachedVersion = new Version(1L);
    final Version expectedVersion = new Version(2L);
    final LocalDateTime activated_on = LocalDateTime.now();

    final Customer expectedInstance = Customer.of(id, name, true, reason);

    final Snapshot<Customer> expectedSnapshot =
            new Snapshot<>(expectedInstance, expectedVersion);

    // cached history
    final CreateCustomerCmd command1 = new CreateCustomerCmd(UUID.randomUUID(), id, name);

    final SnapshotData cachedHistory =
            new SnapshotData(new Version(1), Arrays.asList(new CustomerCreated(id, command1.getName())));

    final SnapshotData nonCachedHistory =
            new SnapshotData(new Version(2), Arrays.asList(new CustomerActivated(reason, activated_on)));

    // prepare

    when(dao.getAll(id.getStringValue())).thenReturn(cachedHistory);
    when(dao.getAllAfterVersion(id.getStringValue(), cachedVersion)).thenReturn(nonCachedHistory);

    final Stack1SnapshotReader<CustomerId, Customer> reader = new Stack1SnapshotReader<>(cache, dao, supplier,
            dependencyInjectionFn, stateTransitionFn);

    cache.put(id, cachedHistory);

    Snapshot<Customer> snapshot = reader.getSnapshot(id);

//			assertThat(snapshot).isEqualTo(expectedSnapshot);

    assertThat(snapshot.getInstance().getId()).isEqualTo(expectedSnapshot.getInstance().getId());
    assertThat(snapshot.getInstance().getName()).isEqualTo(expectedSnapshot.getInstance().getName());
    assertThat(snapshot.getInstance().isActive()).isEqualTo(expectedSnapshot.getInstance().isActive());
    assertThat(snapshot.getInstance().getReason()).isEqualTo(expectedSnapshot.getInstance().getReason());

    verify(dao).getAllAfterVersion(eq(id.getStringValue()), eq(cachedVersion));

    verifyNoMoreInteractions(dao);

  }

}

