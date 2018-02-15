package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.vertx.CrabzillaWebModule
import io.github.crabzilla.vertx.entity.EntityCommandHandlerService
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepository
import io.github.crabzilla.vertx.entity.impl.EntityCommandHandlerServiceImpl
import io.github.crabzilla.vertx.entity.impl.EntityUnitOfWorkRepositoryImpl
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.modules.WebHealthCheck
import io.github.crabzilla.vertx.modules.WriteDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton

@Module(includes = [(CrabzillaWebModule::class)])
class RestServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  fun handlerService() : EntityCommandHandlerService {
    return EntityCommandHandlerServiceImpl(vertx, "example1")
  }

  @Provides @IntoSet
  fun restVerticle(uowRepository: EntityUnitOfWorkRepository, config: JsonObject,
                   handlerService: EntityCommandHandlerService,
                   @WebHealthCheck healthCheckHandler: HealthCheckHandler): EntityCommandRestVerticle {
    return EntityCommandRestVerticle("Customer", config, healthCheckHandler, uowRepository, handlerService)
  }

  @Provides
  @Singleton
  fun uowRepository(@WriteDatabase jdbcClient: JDBCClient): EntityUnitOfWorkRepository {
    return EntityUnitOfWorkRepositoryImpl(jdbcClient)
  }

}
