package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.CommandHandlerService
import io.github.crabzilla.vertx.CommandHandlerServiceImpl
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.modules.CrabzillaModule
import io.github.crabzilla.vertx.verticles.WebVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import javax.inject.Singleton

@Module(includes = [Example1Module::class])
class Example1ServiceModule(vertx: Vertx, config: JsonObject) : CrabzillaModule(vertx, config) {

  @Provides
  @Singleton
  fun handlerService() : CommandHandlerService {
    return CommandHandlerServiceImpl(vertx, subDomainName())
  }

  @Provides
  @Singleton
  fun restVerticle(uowRepository: UnitOfWorkRepository, config: JsonObject,
                   handlerService: CommandHandlerService,
                   healthCheckHandler: HealthCheckHandler): WebVerticle {
    return WebVerticle(subDomainName(), config, healthCheckHandler, uowRepository, handlerService)
  }

}
