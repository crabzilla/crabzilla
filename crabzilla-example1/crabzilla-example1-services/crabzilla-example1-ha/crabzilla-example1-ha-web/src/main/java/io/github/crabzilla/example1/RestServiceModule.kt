package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.entity.service.EntityCommandHandlerService
import io.github.crabzilla.vertx.entity.service.EntityCommandHandlerServiceImpl
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton


// tag::module[]

@Module
class RestServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  fun handlerService() : EntityCommandHandlerService {
    return EntityCommandHandlerServiceImpl(vertx, "example1")
  }

  @Provides @IntoSet
  fun restVerticle(uowRepository: EntityUnitOfWorkRepositoryImpl, config: JsonObject,
                   handlerService: EntityCommandHandlerService):
    EntityCommandRestVerticle {
    return EntityCommandRestVerticle("Customer", config, uowRepository, handlerService)
  }

  @Provides
  @Singleton
  fun uowRepo(@WriteDatabase jdbcClient: JDBCClient): EntityUnitOfWorkRepositoryImpl {
    return EntityUnitOfWorkRepositoryImpl("Customer", jdbcClient)
  }

}
// end::module[]
