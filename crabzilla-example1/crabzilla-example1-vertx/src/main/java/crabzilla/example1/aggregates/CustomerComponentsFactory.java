package crabzilla.example1.aggregates;

import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.services.SampleService;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.Snapshot;
import crabzilla.model.UnitOfWork;
import crabzilla.model.util.Either;
import crabzilla.stack.model.AggregateRootComponentsFactory;
import crabzilla.stack.model.SnapshotMessage;
import crabzilla.stack.vertx.ShareableSnapshot;
import crabzilla.stack.vertx.VertxEventRepository;
import crabzilla.stack.vertx.VertxSnapshotMessageFn;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.jdbc.JDBCClient;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

class CustomerComponentsFactory implements AggregateRootComponentsFactory<Customer> {

  private final SampleService service;
  private final LocalMap<String, ShareableSnapshot<Customer>> localMap;
  private final JDBCClient jdbcClient;

  @Inject
  public CustomerComponentsFactory(SampleService service,
                                   LocalMap<String, ShareableSnapshot<Customer>> localMap,
                                   JDBCClient jdbcClient) {
    this.service = service;
    this.jdbcClient = jdbcClient;
    this.localMap = localMap;
  }

  @Override
  public Supplier<Customer> supplierFn() {
    return new CustomerSupplierFn();
  }

  @Override
  public BiFunction<Event, Customer, Customer> stateTransitionFn() {return new CustomerStateTransitionFn(); }

  @Override
  public Function<Command, List<String>> cmdValidatorFn() {
    return new CustomerCommandValidatorFn();
  }

  @Override
  public BiFunction<Command, Snapshot<Customer>, Either<Exception, Optional<UnitOfWork>>> cmdHandlerFn() {
    return new CustomerCmdHandlerFn(stateTransitionFn(), depInjectionFn());
  }

  @Override
  public Function<Customer, Customer> depInjectionFn() {return (c) -> c.withService(service); }

  @Override
  public Function<String, SnapshotMessage<Customer>> snapshotReaderFn() {
    return new VertxSnapshotMessageFn<>(localMap, new VertxEventRepository(Customer.class.getName(),
            jdbcClient), snaphotFactory());
  }

}
