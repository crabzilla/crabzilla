package io.github.crabzilla.vertx.modules

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.initVertx
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton

@Module(includes = [WriteDbModule::class, ReadDbModule::class])
open class JdbcModule(val vertx: Vertx, val config: JsonObject) {

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
  fun healthcheck(@WriteDatabase jdbcClientWrite: JDBCClient,
                  @ReadDatabase jdbcClientRead: JDBCClient) : HealthCheckHandler {

    val healthCheckHandler = HealthCheckHandler.create(vertx)

    healthCheckHandler.register("databases:write-database",{ future ->
      jdbcClientWrite.getConnection({ connection ->
        if (connection.failed()) {
          connection.cause().printStackTrace()
          future.fail(connection.cause())
        } else {
          connection.result().close()
          future.complete(Status.OK())
        }
      })
    })

    healthCheckHandler.register("databases:read-database",{ future ->
      jdbcClientRead.getConnection({ connection ->
        if (connection.failed()) {
          future.fail(connection.cause())
        } else {
          connection.result().close()
          future.complete(Status.OK())
        }
      })
    })

    return healthCheckHandler

  }

}
