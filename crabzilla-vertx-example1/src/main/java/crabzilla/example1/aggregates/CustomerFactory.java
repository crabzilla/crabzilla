package crabzilla.example1.aggregates;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.services.SampleService;
import crabzilla.model.*;
import crabzilla.vertx.VertxAggregateRootComponentsFactory;
import crabzilla.vertx.repositories.VertxUnitOfWorkRepository;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import lombok.val;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static crabzilla.vertx.util.StringHelper.circuitBreakerId;

public class CustomerFactory implements VertxAggregateRootComponentsFactory<Customer> {

  private final SampleService service;
  private final Vertx vertx;
  private final VertxUnitOfWorkRepository eventStore;

  @Inject
  public CustomerFactory(SampleService service, Vertx vertx,
                         VertxUnitOfWorkRepository eventStore) {
    this.service = service;
    this.vertx = vertx;
    this.eventStore = eventStore;

  }

  @Override
  public Supplier<Customer> supplierFn() {
    return new CustomerSupplierFn();
  }

  @Override
  public Function<Customer, Customer> depInjectionFn() {return (c) -> c.withService(service); }

  @Override
  public BiFunction<Event, Customer, Customer> stateTransitionFn() {return new CustomerStateTransitionFn(); }

  @Override
  public Function<Command, List<String>> cmdValidatorFn() {
    return new CustomerCommandValidatorFn();
  }

  @Override
  public BiFunction<Command, Snapshot<Customer>, Either<Throwable, Optional<UnitOfWork>>> cmdHandlerFn() {
    return new CustomerCmdHandlerFn(instance -> new StateTransitionsTracker<>(instance, stateTransitionFn(), depInjectionFn()));
  }

  @Override
  public Snapshotter<Customer> snapshotter() {
    return new Snapshotter<>(supplierFn(), instance -> new StateTransitionsTracker<>(instance, stateTransitionFn(), depInjectionFn()));
  }

  @Override
  public CommandRestVerticle<Customer> restVerticle() {
    return new CommandRestVerticle<>(vertx, Customer.class);
  }

  @Override
  public CommandHandlerVerticle<Customer> cmdHandlerVerticle() {

    final LoadingCache<String, Snapshot<Customer>> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(key -> null); // TODO you can plug your snapshot here!

    val circuitBreaker = CircuitBreaker.create(circuitBreakerId(Customer.class), vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    return new CommandHandlerVerticle<>(Customer.class, cmdHandlerFn(),
            cmdValidatorFn(), snapshotter(), eventStore, cache, vertx, circuitBreaker);
  }

}
