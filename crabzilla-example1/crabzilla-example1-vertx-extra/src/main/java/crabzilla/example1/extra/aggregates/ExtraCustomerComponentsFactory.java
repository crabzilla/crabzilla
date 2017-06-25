package crabzilla.example1.extra.aggregates;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import crabzilla.example1.services.SampleService;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.Snapshot;
import crabzilla.model.UnitOfWork;
import crabzilla.model.util.Either;
import crabzilla.stack.model.AggregateRootComponentsFactory;
import crabzilla.stack.model.SnapshotMessage;
import crabzilla.stack.vertx.VertxEventRepository;
import crabzilla.stack.vertx.VertxSnapshotReaderFn;
import io.vertx.ext.jdbc.JDBCClient;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An alternative CustomerComponents factory using Vavr
 */
class ExtraCustomerComponentsFactory implements AggregateRootComponentsFactory<Customer> {

  private final SampleService service;
  private final JDBCClient jdbcClient;
  private final Cache<String, Snapshot<Customer>> cache;

  @Inject
  public ExtraCustomerComponentsFactory(SampleService service, JDBCClient jdbcClient) {
    this.service = service;
    this.jdbcClient = jdbcClient;
    this.cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
  }

  @Override
  public Supplier<Customer> supplierFn() {
    return new CustomerSupplierFn();
  }

  @Override
  public BiFunction<Event, Customer, Customer> stateTransitionFn() {return new CustomerStateTransitionFnJavaslang(); }

  @Override
  public Function<Command, List<String>> cmdValidatorFn() {
    return new CustomerCommandValidatorFn();
  }

  @Override
  public BiFunction<Command, Snapshot<Customer>, Either<Exception, Optional<UnitOfWork>>> cmdHandlerFn() {
    return new CustomerCmdHandlerFnJavaslang(stateTransitionFn(), depInjectionFn());
  }

  @Override
  public Function<Customer, Customer> depInjectionFn() {return (c) -> c.withService(service); }

  @Override
  public Cache<String, Snapshot<Customer>> cache() {
    return cache;
  }

  @Override
  public Function<String, SnapshotMessage<Customer>> snapshotReaderFn() {
    return new VertxSnapshotReaderFn<>(cache, new VertxEventRepository(Customer.class, jdbcClient), snaphotFactory());
  }

}
