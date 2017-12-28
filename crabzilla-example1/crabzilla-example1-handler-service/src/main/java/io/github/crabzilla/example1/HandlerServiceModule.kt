package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepository
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


// tag::module[]
@Module(includes = [HandlerModule::class])
class HandlerServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {
  
  @Provides @IntoSet
  fun restVerticle(uowRepository: EntityUnitOfWorkRepository, config: JsonObject):
          EntityCommandRestVerticle<out Any> {
    return EntityCommandRestVerticle(Customer::class.java, config, uowRepository)
  }


}
// end::module[]