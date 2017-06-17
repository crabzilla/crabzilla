package crabzilla.example1.extra.aggregates;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.example1.extra.implementations.CaffeineCommandHandlerVerticle;
import crabzilla.model.Snapshot;
import crabzilla.stack.EventRepository;
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
  Cache<String, Snapshot<Customer>> cache() {
    return Caffeine.newBuilder().build();
  }

  @Provides
  @Singleton
  CommandRestVerticle<Customer> restVerticle(Vertx vertx) {
    return new CommandRestVerticle<>(vertx, Customer.class);
  }

  @Provides
  @Singleton
  CaffeineCommandHandlerVerticle<Customer> handler(ExtraCustomerComponentsFactory f, Vertx vertx, EventRepository eventStore,
                                           @Named("cmd-handler") CircuitBreaker circuitBreaker,
                                           Cache<String, Snapshot<Customer>> cache) {

    return new CaffeineCommandHandlerVerticle<>(Customer.class, f.snapshotReaderFn(), f.cmdHandlerFn(),
            f.cmdValidatorFn(), eventStore, cache, vertx, circuitBreaker);
  }

}
