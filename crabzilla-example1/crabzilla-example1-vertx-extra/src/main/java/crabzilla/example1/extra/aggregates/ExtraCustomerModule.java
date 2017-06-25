package crabzilla.example1.extra.aggregates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.stack.EventRepository;
import crabzilla.stack.vertx.CommandHandlerVerticle;
import crabzilla.stack.vertx.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Vertx;

import javax.inject.Named;

public class ExtraCustomerModule extends AbstractModule {

  @Override
  protected void configure() {

    bind(ExtraCustomerComponentsFactory.class).asEagerSingleton();

  }

  @Provides
  @Singleton
  CommandRestVerticle<Customer> restVerticle(Vertx vertx) {
    return new CommandRestVerticle<>(vertx, Customer.class);
  }

  @Provides
  @Singleton
  CommandHandlerVerticle<Customer> handler(ExtraCustomerComponentsFactory f,
                                           Vertx vertx,
                                           EventRepository eventStore,
                                           @Named("cmd-handler") CircuitBreaker circuitBreaker) {

    return new CommandHandlerVerticle<>(Customer.class, f.snapshotReaderFn(), f.cmdHandlerFn(),
            f.cmdValidatorFn(), eventStore, f.cache(), vertx, circuitBreaker);
  }

}
