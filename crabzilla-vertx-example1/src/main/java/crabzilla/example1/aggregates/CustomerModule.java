package crabzilla.example1.aggregates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import crabzilla.example1.aggregates.customer.Customer;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;

public class CustomerModule extends AbstractModule {

  @Override
  protected void configure() {

    bind(CustomerComponentsFactory.class).asEagerSingleton();

  }

  @Provides
  @Singleton
  CommandRestVerticle<Customer> restVerticle(CustomerComponentsFactory componentsFactory) {

    return componentsFactory.restVerticle();
  }

  @Provides
  @Singleton
  CommandHandlerVerticle<Customer> handler(CustomerComponentsFactory componentsFactory) {

    return componentsFactory.cmdHandlerVerticle();
  }

}
