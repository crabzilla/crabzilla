package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.CommandHandlerService
import io.github.crabzilla.vertx.CommandHandlerServiceImpl
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.modules.JdbcModule
import io.github.crabzilla.vertx.verticles.RestVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import javax.inject.Singleton

@Module
class RestServiceModule(vertx: Vertx, config: JsonObject) : JdbcModule(vertx, config) {

  @Provides
  @Singleton
  fun handlerService() : CommandHandlerService {
    return CommandHandlerServiceImpl(vertx, subDomainName())
  }

  @Provides
  @Singleton
  fun restVerticle(uowRepository: UnitOfWorkRepository, config: JsonObject,
                   handlerService: CommandHandlerService,
                   healthCheckHandler: HealthCheckHandler): RestVerticle {
    return RestVerticle(subDomainName(), config, healthCheckHandler, uowRepository, handlerService)
  }

}
