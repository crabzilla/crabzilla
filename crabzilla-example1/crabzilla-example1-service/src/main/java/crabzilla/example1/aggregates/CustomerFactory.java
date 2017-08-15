package crabzilla.example1.aggregates;


import crabzilla.example1.services.SampleInternalService;
import crabzilla.model.*;
import crabzilla.vertx.AggregateRootComponentsFactory;
import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
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
    return new CustomerFunctions.SupplierFn();
  }

  @Override
  public BiFunction<DomainEvent, Customer, Customer> stateTransitionFn() {return new CustomerFunctions.StateTransitionFn(); }

  @Override
  public Function<EntityCommand, List<String>> cmdValidatorFn() {
    return new CustomerFunctionsVavr.CommandValidatorFn();
  }

  @Override
  public BiFunction<EntityCommand, Snapshot<Customer>, Either<Throwable, Optional<EntityUnitOfWork>>> cmdHandlerFn() {
    return new CustomerFunctions.CommandHandlerFn(instance -> new StateTransitionsTracker<>(instance, stateTransitionFn(), depInjectionFn()));
  }

  @Override
  public Function<Customer, Customer> depInjectionFn() {return (c) -> c.withService(service); }

  @Override
  public JDBCClient jdbcClient() {
    return jdbcClient;
  }

  @Override
  public Vertx vertx() {
    return vertx;
  }

}
