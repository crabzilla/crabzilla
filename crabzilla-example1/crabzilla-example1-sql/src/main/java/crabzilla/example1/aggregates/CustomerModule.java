package crabzilla.example1.aggregates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.aggregates.customer.CustomerCmdHandlerFnJavaslang;
import crabzilla.example1.aggregates.customer.CustomerStateTransitionFnJavaslang;
import crabzilla.example1.aggregates.customer.CustomerSupplierFn;
import crabzilla.model.CommandHandlerFn;
import crabzilla.model.Event;
import crabzilla.stack.AggregateRootModule;
import crabzilla.stack.SnapshotFactory;

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
    return new CustomerSupplierFn();
  }

  @Provides
  @Singleton
  public BiFunction<Event, Customer, Customer> stateTransitionFn() {
    return new CustomerStateTransitionFnJavaslang();
  }

  @Provides
  @Singleton
  public CommandHandlerFn<Customer> cmdHandler(final BiFunction<Event, Customer, Customer> stateTransFn,
                                               final Function<Customer, Customer> depInjectionFn) {
    return new CustomerCmdHandlerFnJavaslang(stateTransFn, depInjectionFn);
  }

  @Provides
  @Singleton
  public SnapshotFactory<Customer> snapshotFactory(Supplier<Customer> supplier,
                                                   Function<Customer, Customer> depInjectionFn,
                                                   BiFunction<Event, Customer, Customer> stateTransFn) {
    return new SnapshotFactory<>(supplier, depInjectionFn, stateTransFn);
  }

}

