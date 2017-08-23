package io.github.crabzilla.example1.customer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import io.github.crabzilla.vertx.verticles.EntityCommandHandlerVerticle;
import io.github.crabzilla.vertx.verticles.EntityCommandRestVerticle;
import io.vertx.core.Verticle;

public class CustomerModule extends AbstractModule {

  @Override
  protected void configure() {

    // to bind aggregate functions
    bind(CustomerFactory.class).asEagerSingleton();

    // to bind verticles for this aggregate
    TypeLiteral<EntityCommandRestVerticle<Customer>> restType =
            new TypeLiteral<EntityCommandRestVerticle<Customer>>() {};
    TypeLiteral<EntityCommandHandlerVerticle<Customer>> handlerType =
            new TypeLiteral<EntityCommandHandlerVerticle<Customer>>() {};

    MapBinder<String, Verticle> mapbinder =
            MapBinder.newMapBinder(binder(), String.class, Verticle.class);

    mapbinder.addBinding("customer.rest").to(restType);
    mapbinder.addBinding("customer.handler").to(handlerType);

  }

  @Provides
  @Singleton
  EntityCommandRestVerticle<Customer> restVerticle(CustomerFactory componentsFactory) {
    return componentsFactory.restVerticle();
  }

  @Provides
  @Singleton
  EntityCommandHandlerVerticle<Customer> handler(CustomerFactory componentsFactory) {
    return componentsFactory.cmdHandlerVerticle();
  }

}
