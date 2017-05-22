package crabzilla.example1.aggregates;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.Event;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stacks.sql.CaffeinedSnapshotReaderFn;
import crabzilla.stacks.sql.SnapshotReaderModule;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomerStackModule extends AbstractModule implements SnapshotReaderModule<Customer> {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  public SnapshotReaderFn<Customer> snapshotReader(final Cache<String, Snapshot<Customer>> cache,
                                                   final EventRepository eventRepo,
                                                   final Supplier<Customer> supplier,
                                                   final Function<Customer, Customer> dependencyInjectionFn,
                                                   final BiFunction<Event, Customer, Customer> stateTransitionFn) {
    return new CaffeinedSnapshotReaderFn<>(cache, eventRepo, supplier, dependencyInjectionFn, stateTransitionFn);
  }

  @Provides
  @Singleton
  public Cache<String, Snapshot<Customer>> cache() {
    return Caffeine.newBuilder().build();
  }

  @Provides
  @Singleton
  public Supplier<Function<Customer, Customer>> depInjectionFnSupplier(Function<Customer, Customer> depInjectionFn) {
    return () -> depInjectionFn;
  }

  @Provides
  @Singleton
  Function<Customer, Customer> dependencyInjectionFn(final SampleServiceImpl service) {
    return (c) -> c.withService(service);
  }

}

