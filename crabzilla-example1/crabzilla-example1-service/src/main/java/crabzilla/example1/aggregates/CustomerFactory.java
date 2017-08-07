package crabzilla.example1.aggregates;


import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.services.SampleInternalService;
import crabzilla.model.*;
import crabzilla.vertx.VertxAggregateRootComponentsFactory;
import crabzilla.vertx.repositories.VertxUnitOfWorkRepository;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.val;
import net.jodah.expiringmap.ExpiringMap;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static crabzilla.vertx.util.StringHelper.circuitBreakerId;

public class CustomerFactory implements VertxAggregateRootComponentsFactory<Customer> {

  private final SampleInternalService service;
  private final Vertx vertx;
  private final JDBCClient jdbcClient;

  @Inject
  public CustomerFactory(SampleInternalService service, Vertx vertx, JDBCClient jdbcClient) {
    this.service = service;
    this.vertx = vertx;
    this.jdbcClient = jdbcClient;
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
    return new CommandRestVerticle<>(Customer.class);
  }

  @Override
  public CommandHandlerVerticle<Customer> cmdHandlerVerticle() {

    final ExpiringMap<String, Snapshot<Customer>> cache = ExpiringMap.builder()
                                                    .expiration(5, TimeUnit.MINUTES)
                                                    .maxSize(10_000)
                                                    .build();
    val circuitBreaker = CircuitBreaker.create(circuitBreakerId(Customer.class), vertx,
            new CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    );

    return new CommandHandlerVerticle<>(Customer.class, cmdHandlerFn(),
            cmdValidatorFn(), snapshotter(), uowRepository(), cache, circuitBreaker);
  }

  @Override
  public VertxUnitOfWorkRepository uowRepository() {
    return new VertxUnitOfWorkRepository(Customer.class, jdbcClient);
  }

}
