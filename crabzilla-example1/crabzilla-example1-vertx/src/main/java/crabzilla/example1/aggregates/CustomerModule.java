package crabzilla.example1.aggregates;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerCmdHandlerFnJavaslang;
import crabzilla.example1.aggregates.customer.CustomerStateTransitionFnJavaslang;
import crabzilla.example1.aggregates.customer.CustomerSupplierFn;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.*;
import crabzilla.stack.AggregateRootModule;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotFactory;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.vertx.sql.CaffeinedSnapshotReaderFn;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomerModule extends AbstractModule implements AggregateRootModule<Customer> {

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  public Supplier<Customer> supplier() {
    return new CustomerSupplierFn();
  }

  @Provides
  @Singleton
  public BiFunction<Event, Customer, Customer> stateTransitionFn() {
    return new CustomerStateTransitionFnJavaslang();
  }

  @Provides
  @Singleton
  public SnapshotFactory<Customer> snapshotFactory(Supplier<Customer> supplier, Supplier<Function<Customer, Customer>> depInjectionFn, BiFunction<Event, Customer, Customer> stateTransFn) {
    return new SnapshotFactory<>(supplier, depInjectionFn.get(), stateTransFn);
  }

  @Provides
  @Singleton
  public CommandHandlerFn<Customer> cmdHandlerFn(BiFunction<Event, Customer, Customer> stateTransFn, Supplier<Function<Customer, Customer>> depInjectionFn) {
    return new CustomerCmdHandlerFnJavaslang(stateTransFn, depInjectionFn.get());
  }

  // dep injection

  @Provides
  @Singleton
  public Supplier<Function<Customer, Customer>> depInjectionFnSupplier(Function<Customer, Customer> depInjectionFn) {
    return () -> depInjectionFn;
  }

  @Provides
  @Singleton
  Function<Customer, Customer> depInjectionFn(final SampleServiceImpl service) {
    return (c) -> c.withService(service);
  }

  // validator

  @Provides
  @Singleton
  public Supplier<CommandValidatorFn> cmdValidatorSupplier(CommandValidatorFn cmdValidator) {
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

  // snapshotReader

  @Provides
  @Singleton
  public Supplier<SnapshotReaderFn<Customer>> snapshotReaderFn(SnapshotReaderFn<Customer> snapshotReaderFn) {
    return () -> snapshotReaderFn;
  }

  @Provides
  @Singleton
  public SnapshotReaderFn<Customer> snapshotReader(final Cache<String, Snapshot<Customer>> cache,
                                                   final EventRepository eventRepo,
                                                   final SnapshotFactory<Customer> snapshotFactory) {
    return new CaffeinedSnapshotReaderFn<>(cache, eventRepo, snapshotFactory);
  }

  // external

  @Provides
  @Singleton
  public Cache<String, Snapshot<Customer>> cache() {
    return Caffeine.newBuilder().build();
  }

}

