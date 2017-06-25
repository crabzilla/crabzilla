//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//import crabzilla.example1.aggregates.customer.Customer;
//import crabzilla.example1.aggregates.customer.CustomerId;
//import crabzilla.example1.aggregates.customer.CustomerStateTransitionFnJavaslang;
//import crabzilla.example1.aggregates.customer.CustomerSupplierFn;
//import crabzilla.example1.aggregates.customer.commands.CreateCustomerCmd;
//import crabzilla.example1.aggregates.customer.events.CustomerActivated;
//import crabzilla.example1.aggregates.customer.events.CustomerCreated;
//import crabzilla.model.Snapshot;
//import crabzilla.model.SnapshotFactory;
//import crabzilla.model.Version;
//import crabzilla.vertx.repositories.VertxEventRepository;
//import lombok.val;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//
//import java.time.Instant;
//import java.util.Collections;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.concurrent.ExecutionException;
//
//import static java.util.Arrays.asList;
//import static java.util.Collections.singletonList;
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.mockito.Mockito.*;
//import static org.mockito.MockitoAnnotations.initMocks;
//
//@DisplayName("A VertxSnapshotReaderFn")
//public class VertxSnapshotReaderFnTest {
//
//  SnapshotFactory<Customer> snapshotFactory;
//
//  Cache<String, Snapshot<Customer>> cache;
//
//  @Mock
//  VertxEventRepository eventRepository;
//
//  @BeforeEach
//  public void init() throws Exception {
//    initMocks(this);
//    snapshotFactory = new SnapshotFactory<>(new CustomerSupplierFn(), c -> c, new CustomerStateTransitionFnJavaslang());
//    cache = Caffeine.newBuilder().build();
//  }
//
//  @Test
//  public void on_empty_history_then_returns_version_0() throws ExecutionException {
//
//    val id = new CustomerId("customer#1");
//
//    val expectedSnapshot = snapshotFactory.getEmptySnapshot();
//
//    when(eventRepository.getAll(id.getStringValue())).thenReturn(Optional.empty());
//
//    val reader = new VertxSnapshotReaderFn<Customer>(cache, eventRepository, snapshotFactory);
//
//    assertThat(expectedSnapshot).isEqualTo(reader.apply(id.getStringValue()).getSnapshot());
//
//    verify(eventRepository).getAll(id.getStringValue());
//
//    verifyNoMoreInteractions(eventRepository);
//
//  }
//
//  @Test
//  public void on_empty_cache_then_returns_version_from_db() {
//
//    final CustomerId id = new CustomerId("customer#1");
//    final String name = "customer#1 name";
//
//    val expectedInstance = Customer.of(id, name, false, null);
//    val expectedSnapshot = new Snapshot<>(expectedInstance, Version.create(1L));
//    val command = new CreateCustomerCmd(UUID.randomUUID(), id, name);
//    val expectedSnapshotData =
//            new EventRepository.SnapshotData(new Version(1), singletonList(new CustomerCreated(id, command.getName())));
//
//    when(eventRepository.getAll(id.getStringValue())).thenReturn(Optional.of(expectedSnapshotData));
//
//    val reader = new VertxSnapshotReaderFn<Customer>(cache, eventRepository, snapshotFactory);
//
//    val resultingSnapshotMsg = reader.apply(id.getStringValue());
//
//    assertThat(expectedSnapshot).isEqualTo(resultingSnapshotMsg.getSnapshot());
//
//    verify(eventRepository).getAll(id.getStringValue());
//
//    assertThat(snapshotFactory.createSnapshot(expectedSnapshotData)).isEqualTo(resultingSnapshotMsg.getSnapshot());
//
//    verifyNoMoreInteractions(eventRepository);
//
//  }
//
//  @Test
//  public void on_cache_then_hits_db_to_check_newer_version() {
//
//    val id = new CustomerId("customer#1");
//    val name = "customer#1 name";
//    val command = new CreateCustomerCmd(UUID.randomUUID(), id, name);
//    val expectedSnapshotData = new EventRepository.SnapshotData(new Version(1),
//            Collections.singletonList(new CustomerCreated(id, command.getName())));
//
//    // prepare
//
//    when(eventRepository.getAll(id.getStringValue())).thenReturn(Optional.of(expectedSnapshotData));
//
//    cache.put(id.getStringValue(), snapshotFactory.createSnapshot(expectedSnapshotData));
//
//    // run
//
//    val reader = new VertxSnapshotReaderFn<Customer>(cache, eventRepository, snapshotFactory);
//
//    val resultingSnapshotMsg = reader.apply(id.getStringValue());
//
//    // verify
//
//    assertThat(snapshotFactory.createSnapshot(expectedSnapshotData)).isEqualTo(resultingSnapshotMsg.getSnapshot());
//
//    verify(eventRepository).selectAfterVersion(id.getStringValue(), expectedSnapshotData.getVersion());
//
//    verifyNoMoreInteractions(eventRepository);
//
//  }
//
//  @Test
//  public void on_both_cache_and_db_then_hits_db_to_compose_history() {
//
//    val id = new CustomerId("customer#1");
//    val name = "customer#1 name";
//    val reason = "because yes";
//
//    val cachedVersion = new Version(1L);
//    val expectedVersion = new Version(2L);
//
//    val cachedInstance = Customer.of(id, name, false, null);
//    val expectedInstance = Customer.of(id, name, true, reason);
//
//    val cachedSnapshot = new Snapshot<Customer>(cachedInstance, cachedVersion);
//    val expectedSnapshot = new Snapshot<Customer>(expectedInstance, expectedVersion);
//
//    val activated_on = Instant.now();
//    val nonCachedHistory = new EventRepository.SnapshotData(new Version(2), asList(new CustomerActivated(reason, activated_on)));
//
//    // prepare
//
//    cache.put(id.getStringValue(), cachedSnapshot);
//
//    when(eventRepository.selectAfterVersion(id.getStringValue(), cachedVersion)).thenReturn(Optional.of(nonCachedHistory));
//
//    val reader = new VertxSnapshotReaderFn<Customer>(cache, eventRepository, snapshotFactory);
//
//    // run
//
//    val resultingSnapshotMsg = reader.apply(id.getStringValue());
//
//    assertThat(expectedSnapshot).isEqualTo(resultingSnapshotMsg.getSnapshot());
//
//    verify(eventRepository).selectAfterVersion(eq(id.getStringValue()), eq(cachedVersion));
//
//    verifyNoMoreInteractions(eventRepository);
//
//  }
//
//}
//
