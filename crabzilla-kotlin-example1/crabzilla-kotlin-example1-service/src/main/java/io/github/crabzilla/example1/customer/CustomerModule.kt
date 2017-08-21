package io.github.crabzilla.example1.customer

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder
import io.vertx.core.Verticle
import io.github.crabzilla.vertx.verticles.CommandHandlerVerticle
import io.github.crabzilla.vertx.verticles.CommandRestVerticle

class CustomerModule : AbstractModule() {

  override fun configure() {

    // to bind aggregate functions
    bind(CustomerFactory::class.java).asEagerSingleton()

    // to bind verticles for this aggregate
    val restType = object : TypeLiteral<CommandRestVerticle<Customer>>() {

    }
    val handlerType = object : TypeLiteral<CommandHandlerVerticle<Customer>>() {

    }

    val mapbinder = MapBinder.newMapBinder(binder(), String::class.java, Verticle::class.java)

    mapbinder.addBinding("customer.rest").to(restType)
    mapbinder.addBinding("customer.handler").to(handlerType)

  }


  @Provides
  @Singleton
  internal fun restVerticle(componentsFactory: CustomerFactory): CommandRestVerticle<Customer> {
    return componentsFactory.restVerticle()
  }

  @Provides
  @Singleton
  internal fun handler(componentsFactory: CustomerFactory): CommandHandlerVerticle<Customer> {
    return componentsFactory.cmdHandlerVerticle()
  }

}
