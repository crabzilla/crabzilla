package io.github.crabzilla.vertx.modules

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.initVertx
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton

// tag::module[]
@Module(includes = arrayOf(WriteDbModule::class, ReadDbModule::class))
open class CrabzillaModule(val vertx: Vertx, val config: JsonObject) {

  init {

    System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
    LoggerFactory.getLogger(LoggerFactory::class.java) // Required for Logback to work in Vertx

    initVertx(vertx)

  }

  @Provides
  @Singleton
  fun vertx(): Vertx {
    return vertx
  }

  @Provides
  @Singleton
  fun config(): JsonObject {
    return config
  }

  @Provides
  @Singleton
  fun healthcheck(jdbcClientWrite: JDBCClient, jdbcClientRead: JDBCClient): HealthCheckHandler {

    val healthCheckHandler = HealthCheckHandler.create(vertx)

    // TODO
    healthCheckHandler.register("health-write-database", { f -> f.complete(Status.OK()) })
    healthCheckHandler.register("health-read-database", { f -> f.complete(Status.OK()) })
    healthCheckHandler.register("health-handler", { f -> f.complete(Status.OK()) })
    healthCheckHandler.register("health-projector", { f -> f.complete(Status.OK()) })

    return healthCheckHandler
  }

}
// end::module[]
