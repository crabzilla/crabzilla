package crabzilla.example1.aggregates;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import crabzilla.vertx.verticles.CommandHandlerVerticle;
import crabzilla.vertx.verticles.CommandRestVerticle;
import io.vertx.core.Verticle;

public class CustomerModule extends AbstractModule {

  @Override
  protected void configure() {

    // to bind aggregate functions
    bind(CustomerFactory.class).asEagerSingleton();

    // to bind verticles for this aggregate
    TypeLiteral<CommandRestVerticle<Customer>> restType = new TypeLiteral<CommandRestVerticle<Customer>>() {};
    TypeLiteral<CommandHandlerVerticle<Customer>> handlerType = new TypeLiteral<CommandHandlerVerticle<Customer>>() {};

    MapBinder<String, Verticle> mapbinder = MapBinder.newMapBinder(binder(), String.class, Verticle.class);

    mapbinder.addBinding("customer.rest").to(restType);
    mapbinder.addBinding("customer.handler").to(handlerType);

  }

  @Provides
  @Singleton
  CommandRestVerticle<Customer> restVerticle(CustomerFactory componentsFactory) {
    return componentsFactory.restVerticle();
  }

  @Provides
  @Singleton
  CommandHandlerVerticle<Customer> handler(CustomerFactory componentsFactory) {
    return componentsFactory.cmdHandlerVerticle();
  }

}
