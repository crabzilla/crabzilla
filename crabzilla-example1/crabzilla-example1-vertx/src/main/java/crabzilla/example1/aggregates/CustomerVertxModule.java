package crabzilla.example1.aggregates;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.Command;
import crabzilla.model.CommandValidatorFn;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotFactory;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stacks.sql.CaffeinedSnapshotReaderFn;
import crabzilla.stacks.sql.SnapshotReaderModule;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomerVertxModule extends AbstractModule implements SnapshotReaderModule<Customer> {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  public SnapshotReaderFn<Customer> snapshotReader(final Cache<String, Snapshot<Customer>> cache,
                                                   final EventRepository eventRepo,
                                                   final SnapshotFactory<Customer> snapshotFactory) {
    return new CaffeinedSnapshotReaderFn<>(cache, eventRepo, snapshotFactory);
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

  @Provides
  @Singleton
  public Supplier<CommandValidatorFn> depInjectionFnSupplier(CommandValidatorFn cmdValidator) {
    return () -> cmdValidator;
  }

  @Provides
  @Singleton
  CommandValidatorFn cmdValidator() {
    return new CommandValidatorFn() {
      @Override
      public Optional<String> constraintViolation(Command command) {
        return Optional.empty(); // TODO
      }
    };
  }

}

