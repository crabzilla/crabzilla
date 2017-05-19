package crabzilla.example1.aggregates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerCmdHandler;
import crabzilla.example1.aggregates.customer.CustomerStateTransitionFn;
import crabzilla.example1.services.SampleServiceImpl;
import crabzilla.model.AggregateRootCmdHandler;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.stack.AggregateRootModule;

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
    return () -> new Customer(null, null,  null, false, null);
  }

  @Provides
  @Singleton
  public Supplier<Function<Customer, Customer>> depInjectionFnSupplier(Function<Customer, Customer> depInjectionFn) {
    return () -> depInjectionFn;
  }

  @Provides
  @Singleton
  Function<Customer, Customer> dependencyInjectionFn(final SampleServiceImpl service) {
    return (c) -> c.withService(service);
  }

  @Provides
  @Singleton
  public BiFunction<Event, Customer, Customer> stateTransitionFn() {
    return new CustomerStateTransitionFn();
  }

  @Provides
  @Singleton
  public AggregateRootCmdHandler<Customer> cmdHandler(final BiFunction<Event, Customer, Customer> stateTransFn,
                                                      final Function<Customer, Customer> depInjectionFn) {
    return new CustomerCmdHandler(stateTransFn, depInjectionFn);
  }

  @Provides
  @Singleton
  public Function<Event, Optional<Command>> eventMonitoringFn() {
    return event -> Optional.empty(); // TODO
  }

}
