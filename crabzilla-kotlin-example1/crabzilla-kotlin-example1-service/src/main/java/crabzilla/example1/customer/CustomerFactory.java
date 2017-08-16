package crabzilla.example1.customer;


import crabzilla.example1.SampleInternalService;
import crabzilla.example1.services.SampleInternalServiceImpl;
import crabzilla.vertx.AggregateRootComponentsFactory;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomerFactory implements AggregateRootComponentsFactory<Customer> {

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
  public Class<Customer> clazz() {
    return Customer.class;
  }

  @Override
  public Supplier<Customer> supplierFn() {
    return () -> depInjectionFn().apply(new Customer(null, null, false, null, new SampleInternalServiceImpl()));
  }

  @Override
  public BiFunction<DomainEvent, Customer, Customer> stateTransitionFn() {return new StateTransitionFn(); }

  @Override
  public Function<EntityCommand, List<String>> cmdValidatorFn() {
    return entityCommand -> Collections.emptyList();
    //return new CommandValidatorFn();
  }

  @Override
  public BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> cmdHandlerFn() {
    return new CommandHandlerFn(instance -> new StateTransitionsTracker<>(instance, stateTransitionFn(), depInjectionFn()));
  }

  @Override
  public Function<Customer, Customer> depInjectionFn() {
    return (c) -> c; }

  @Override
  public JDBCClient jdbcClient() {
    return jdbcClient;
  }

  @Override
  public Vertx vertx() {
    return vertx;
  }

}
