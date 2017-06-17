package crabzilla.example1.extra.aggregates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.example1.aggregates.customer.*;
import crabzilla.example1.extra.implementations.CaffeineSnapshotMessageFn;
import crabzilla.example1.extra.implementations.JdbiJacksonEventRepository;
import crabzilla.example1.services.SampleService;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.Snapshot;
import crabzilla.model.UnitOfWork;
import crabzilla.model.util.Either;
import crabzilla.stack.model.AggregateRootComponentsFactory;
import crabzilla.stack.model.SnapshotMessage;
import org.skife.jdbi.v2.DBI;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An alternative CustomerComponents factory using Caffeine, JDBI and Vavr
 */
class ExtraCustomerComponentsFactory implements AggregateRootComponentsFactory<Customer> {

  private final SampleService service;
  private final ObjectMapper jackson;
  private final DBI jdbi;
  private final Cache<String, Snapshot<Customer>> cache;

  @Inject
  public ExtraCustomerComponentsFactory(SampleService service, ObjectMapper jackson, DBI jdbi,
                                        Cache<String, Snapshot<Customer>> cache) {
    this.service = service;
    this.jackson = jackson;
    this.jdbi = jdbi;
    this.cache = cache;
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
    return new CaffeineSnapshotMessageFn<>(cache, new JdbiJacksonEventRepository(Customer.class.getName(),
            jackson, jdbi), snaphotFactory());
  }

}
