package crabzilla.stack.vertx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.example1.aggregates.customer.CustomerStateTransitionFnJavaslang;
import crabzilla.example1.aggregates.customer.CustomerSupplierFn;
import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.model.Snapshot;
import crabzilla.model.Version;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotData;
import crabzilla.stack.SnapshotFactory;
import crabzilla.stack.vertx.sql.CaffeinedSnapshotReaderFn;
import lombok.val;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

@DisplayName("A SnapshotReaderFn")
public class CaffeinedSnapshotReaderFnTest {

  SnapshotFactory<Customer> snapshotFactory;

  Cache<String, Snapshot<Customer>> cache;

  @Mock
  EventRepository eventRepository;

  @BeforeEach
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);
    snapshotFactory = new SnapshotFactory<>(new CustomerSupplierFn(), c -> c, new CustomerStateTransitionFnJavaslang());
    cache = Caffeine.newBuilder().build();
  }

  @Test
  public void on_empty_history_then_returns_version_0() throws ExecutionException {

    val id = new CustomerId("customer#1");

    val expectedSnapshot = snapshotFactory.getEmptySnapshot();

    when(eventRepository.getAll(id.getStringValue())).thenReturn(Optional.empty());

    val reader = new CaffeinedSnapshotReaderFn<>(cache, eventRepository, snapshotFactory);

    AssertionsForClassTypes.assertThat(expectedSnapshot).isEqualTo(reader.getSnapshotMessage(id.getStringValue()).getSnapshot());

    verify(eventRepository).getAll(id.getStringValue());

    verifyNoMoreInteractions(eventRepository);

  }

  @Test
  public void on_empty_cache_then_returns_version_from_db() {

    final CustomerId id = new CustomerId("customer#1");
    final String name = "customer#1 name";

    val expectedInstance = Customer.of(id, name, false, null);
    val expectedSnapshot = new Snapshot<>(expectedInstance, Version.create(1L));
    val command = new CreateCustomerCmd(UUID.randomUUID(), id, name);
    val expectedSnapshotData =
            new SnapshotData(new Version(1), singletonList(new CustomerCreated(id, command.getName())));

    when(eventRepository.getAll(id.getStringValue())).thenReturn(Optional.of(expectedSnapshotData));

    val reader = new CaffeinedSnapshotReaderFn<>(cache, eventRepository, snapshotFactory);

    val resultingSnapshotMsg = reader.getSnapshotMessage(id.getStringValue());

    AssertionsForClassTypes.assertThat(expectedSnapshot).isEqualTo(resultingSnapshotMsg.getSnapshot());

    verify(eventRepository).getAll(id.getStringValue());

    AssertionsForClassTypes.assertThat(snapshotFactory.createSnapshot(expectedSnapshotData)).isEqualTo(resultingSnapshotMsg.getSnapshot());

    verifyNoMoreInteractions(eventRepository);

  }

  @Test
  public void on_cache_then_hits_db_to_check_newer_version() {

    val id = new CustomerId("customer#1");
    val name = "customer#1 name";
    val command = new CreateCustomerCmd(UUID.randomUUID(), id, name);
    val expectedSnapshotData = new SnapshotData(new Version(1),
            Collections.singletonList(new CustomerCreated(id, command.getName())));

    // prepare

    when(eventRepository.getAll(id.getStringValue())).thenReturn(Optional.of(expectedSnapshotData));

    cache.put(id.getStringValue(), snapshotFactory.createSnapshot(expectedSnapshotData));

    // run

    val reader = new CaffeinedSnapshotReaderFn<>(cache, eventRepository, snapshotFactory);

    val resultingSnapshotMsg = reader.getSnapshotMessage(id.getStringValue());

    // verify

    AssertionsForClassTypes.assertThat(snapshotFactory.createSnapshot(expectedSnapshotData)).isEqualTo(resultingSnapshotMsg.getSnapshot());

    verify(eventRepository).getAllAfterVersion(id.getStringValue(), expectedSnapshotData.getVersion());

    verifyNoMoreInteractions(eventRepository);

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

    val activated_on = LocalDateTime.now();
    val nonCachedHistory = new SnapshotData(new Version(2), asList(new CustomerActivated(reason, activated_on)));

    // prepare

    cache.put(id.getStringValue(), cachedSnapshot);

    when(eventRepository.getAllAfterVersion(id.getStringValue(), cachedVersion)).thenReturn(Optional.of(nonCachedHistory));

    val reader = new CaffeinedSnapshotReaderFn<>(cache, eventRepository, snapshotFactory);

    // run

    val resultingSnapshotMsg = reader.getSnapshotMessage(id.getStringValue());

    AssertionsForClassTypes.assertThat(expectedSnapshot).isEqualTo(resultingSnapshotMsg.getSnapshot());

    verify(eventRepository).getAllAfterVersion(eq(id.getStringValue()), eq(cachedVersion));

    verifyNoMoreInteractions(eventRepository);

  }

}

