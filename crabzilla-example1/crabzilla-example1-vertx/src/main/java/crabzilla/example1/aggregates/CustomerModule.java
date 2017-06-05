package crabzilla.example1.aggregates;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.CommandHandlerFn;
import crabzilla.model.CommandValidatorFn;
import crabzilla.model.Event;
import crabzilla.model.Snapshot;
import crabzilla.stack.AggregateRootModule;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotFactory;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.vertx.CaffeinedSnapshotReaderFn;
import crabzilla.stack.vertx.verticles.CommandHandlerVerticle;
import crabzilla.stack.vertx.verticles.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Vertx;

import javax.inject.Named;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomerModule extends AbstractModule implements AggregateRootModule<Customer> {

  @Override
  protected void configure() {
    bind(new TypeLiteral<SnapshotFactory<Customer>>() {;});
    bind(new TypeLiteral<SnapshotReaderFn<Customer>>() {;})
            .to(new TypeLiteral<CaffeinedSnapshotReaderFn<Customer>>() {;});
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
  public CommandHandlerFn<Customer> cmdHandlerFn(Function<Customer, Customer> depInjectionFn,
                                                 BiFunction<Event, Customer, Customer> stateTransFn) {
    return new CustomerCmdHandlerFnJavaslang(stateTransFn, depInjectionFn);
  }

  // outside the interface but necessary to run

  @Provides
  @Singleton
  Function<Customer, Customer> depInjectionFn(final SampleServiceImpl service) {
    return (c) -> c.withService(service);
  }

  @Provides
  @Singleton
  CommandValidatorFn cmdValidator() {
    return new CustomerCommandValidatorFn();
  }

  @Provides
  @Singleton
  CommandHandlerVerticle<Customer> handler(SnapshotReaderFn<Customer> snapshotReaderFn,
                                           CommandHandlerFn<Customer> cmdHandler,
                                           CommandValidatorFn validatorFn,
                                           EventRepository eventStore, Vertx vertx,
                                           Cache<String, Snapshot<Customer>> cache,
                                           @Named("cmd-handler") CircuitBreaker circuitBreaker) {
    return new CommandHandlerVerticle<>(Customer.class, snapshotReaderFn, cmdHandler, validatorFn, eventStore,
            cache, vertx, circuitBreaker);
  }

  @Provides
  @Singleton
  Cache<String, Snapshot<Customer>> cache() {
    return Caffeine.newBuilder().build();
  }

  @Provides
  @Singleton
  CommandRestVerticle<Customer> restVerticle() {
    return new CommandRestVerticle<>(Customer.class);
  }

}
