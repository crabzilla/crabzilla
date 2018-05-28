package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.modules.JdbcModule
import io.github.crabzilla.vertx.verticles.HealthVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import javax.inject.Singleton


@Module(includes = [Example1Module::class])
class HandlerServiceModule(vertx: Vertx, config: JsonObject) : JdbcModule(vertx, config) {

  @Provides
  @Singleton
  fun healthVerticle(config: JsonObject, healthCheckHandler: HealthCheckHandler): HealthVerticle {
    return HealthVerticle(subDomainName(), config.getInteger("HANDLER_HTTP_PORT")!!, healthCheckHandler)
  }

}
