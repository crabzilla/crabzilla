package io.github.crabzilla.example1.customer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle;
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;

// tag::module[]
public class CustomerModule extends AbstractModule {

  @Override
  protected void configure() {

    // to bind aggregate functions
    bind(CustomerFactory.class).asEagerSingleton();

    MapBinder<String, Verticle> mapbinder =
            MapBinder.newMapBinder(binder(), String.class, Verticle.class);

    // to bind verticles for this aggregate
    TypeLiteral<EntityCommandRestVerticle<Customer>> restType =
            new TypeLiteral<EntityCommandRestVerticle<Customer>>() {};
    TypeLiteral<EntityCommandHandlerVerticle<Customer>> handlerType =
            new TypeLiteral<EntityCommandHandlerVerticle<Customer>>() {};

    mapbinder.addBinding("customer.rest").to(restType);
    mapbinder.addBinding("customer.handler").to(handlerType);

  }

  @Provides
  @Singleton
  EntityCommandRestVerticle<Customer> restVerticle(CustomerFactory componentsFactory, JsonObject config) {
    return componentsFactory.restVerticle(config);
  }

  @Provides
  @Singleton
  EntityCommandHandlerVerticle<Customer> handler(CustomerFactory componentsFactory) {
    return componentsFactory.cmdHandlerVerticle();
  }

}
// end::module[]