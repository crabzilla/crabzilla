package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.vertx.*
import io.github.crabzilla.vertx.impl.CommandHandlerServiceImpl
import io.github.crabzilla.vertx.impl.UnitOfWorkRepositoryImpl
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton

@Module(includes = [(CrabzillaWebModule::class)])
class RestServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  fun handlerService() : CommandHandlerService {
    return CommandHandlerServiceImpl(vertx, subDomainName())
  }

  @Provides @IntoSet
  fun restVerticle(uowRepository: UnitOfWorkRepository, config: JsonObject,
                   handlerService: CommandHandlerService,
                   @WebHealthCheck healthCheckHandler: HealthCheckHandler): CommandRestVerticle {
    return CommandRestVerticle(subDomainName(), config, healthCheckHandler, uowRepository, handlerService)
  }

  @Provides
  @Singleton
  fun uowRepository(@WriteDatabase jdbcClient: JDBCClient): UnitOfWorkRepository {
    return UnitOfWorkRepositoryImpl(jdbcClient)
  }

}
