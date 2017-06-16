package crabzilla.example1.aggregates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.stack.EventRepository;
import crabzilla.stack.vertx.ShareableSnapshot;
import crabzilla.stack.vertx.verticles.CommandHandlerVerticle;
import crabzilla.stack.vertx.verticles.CommandRestVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import lombok.val;

import javax.inject.Named;

public class CustomerModule extends AbstractModule {

  @Override
  protected void configure() {

    bind(CustomerComponentsFactory.class).asEagerSingleton();

  }

  @Provides
  @Singleton
  LocalMap<String, ShareableSnapshot<Customer>> localMap(Vertx vertx) {
    val sharedData = vertx.sharedData();
    return sharedData.getLocalMap("CustomerSnapshots");
  }

  @Provides
  @Singleton
  CommandRestVerticle<Customer> restVerticle(Vertx vertx) {
    return new CommandRestVerticle<>(vertx, Customer.class);
  }

  @Provides
  @Singleton
  CommandHandlerVerticle<Customer> handler(CustomerComponentsFactory f, Vertx vertx, EventRepository eventStore,
                                           @Named("cmd-handler") CircuitBreaker circuitBreaker,
                                           LocalMap<String, ShareableSnapshot<Customer>> localMap) {

    return new CommandHandlerVerticle<>(Customer.class, f.snapshotReaderFn(), f.cmdHandlerFn(),
            f.cmdValidatorFn(), eventStore, localMap, vertx, circuitBreaker);
  }

}
