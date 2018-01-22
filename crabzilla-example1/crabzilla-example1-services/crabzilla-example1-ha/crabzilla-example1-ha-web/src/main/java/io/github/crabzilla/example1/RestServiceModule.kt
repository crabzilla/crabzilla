package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.vertx.EntityUnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.entity.service.EntityCommandHandlerService
import io.github.crabzilla.vertx.entity.service.EntityCommandHandlerServiceImpl
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton


// tag::module[]

@Module
class RestServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  fun handlerService(verx: Vertx) : EntityCommandHandlerService {
    return EntityCommandHandlerServiceImpl(vertx, "example1-events")
  }

  @Provides @IntoSet
  fun restVerticle(uowRepository: EntityUnitOfWorkRepositoryImpl, config: JsonObject,
                   handlerService: EntityCommandHandlerService):
    EntityCommandRestVerticle<out Any> {
    return EntityCommandRestVerticle(Customer::class.java, config, uowRepository, handlerService)
  }

  @Provides
  @Singleton
  fun uowRepo(@WriteDatabase jdbcClient: JDBCClient): EntityUnitOfWorkRepositoryImpl {
    return EntityUnitOfWorkRepositoryImpl(jdbcClient)
  }

}
// end::module[]
