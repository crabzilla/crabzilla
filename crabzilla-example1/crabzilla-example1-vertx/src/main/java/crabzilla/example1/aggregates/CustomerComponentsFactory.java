package crabzilla.example1.aggregates;

import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.services.SampleService;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.Snapshot;
import crabzilla.model.UnitOfWork;
import crabzilla.model.util.Either;
import crabzilla.stack.model.AggregateRootComponentsFactory;
import crabzilla.stack.model.CaffeinedSnapshotReaderFn;
import crabzilla.stack.model.SnapshotMessage;
import crabzilla.stack.vertx.JdbiJacksonEventRepository;
import org.skife.jdbi.v2.DBI;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

class CustomerComponentsFactory implements AggregateRootComponentsFactory<Customer> {

  private final SampleService service;
  private final ObjectMapper jackson;
  private final DBI jdbi;

  @Inject
  public CustomerComponentsFactory(SampleService service, ObjectMapper jackson, DBI jdbi) {
    this.service = service;
    this.jackson = jackson;
    this.jdbi = jdbi;
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
  public Function<String, SnapshotMessage<Customer>> snapshotReaderFn() {
    return new CaffeinedSnapshotReaderFn<>(cache(),
            new JdbiJacksonEventRepository(Customer.class.getName(), jackson, jdbi), snaphotFactory());
  }

}
