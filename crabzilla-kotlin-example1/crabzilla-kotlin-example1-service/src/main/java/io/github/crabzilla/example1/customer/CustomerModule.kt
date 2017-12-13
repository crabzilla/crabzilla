package io.github.crabzilla.example1.customer

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle
import io.github.crabzilla.vertx.entity.EntityCommandHttpRpcVerticle
import io.vertx.core.Verticle
import io.vertx.core.json.JsonObject


// tag::module[]
class CustomerModule : AbstractModule() {

  override fun configure() {
    // to bind aggregate functions
    bind(CustomerFactory::class.java).asEagerSingleton()
    // to bind verticles for this aggregate
    val restType = object : TypeLiteral<EntityCommandHttpRpcVerticle<Customer>>() {}
    val handlerType = object : TypeLiteral<EntityCommandHandlerVerticle<Customer>>() {}
    val mapbinder = MapBinder.newMapBinder(binder(), String::class.java, Verticle::class.java)
    mapbinder.addBinding("customer.rest").to(restType)
    mapbinder.addBinding("customer.handler").to(handlerType)
  }

  @Provides
  @Singleton
  internal fun restVerticle(config: JsonObject, componentsFactory: CustomerFactory): EntityCommandHttpRpcVerticle<Customer> {
    return EntityCommandHttpRpcVerticle(Customer::class.java, config, componentsFactory.uowRepository())
  }

  @Provides
  @Singleton
  internal fun handler(componentsFactory: CustomerFactory): EntityCommandHandlerVerticle<Customer> {
    return componentsFactory.cmdHandlerVerticle()
  }

}
// end::module[]