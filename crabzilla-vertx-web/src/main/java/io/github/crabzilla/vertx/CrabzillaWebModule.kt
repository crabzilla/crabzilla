package io.github.crabzilla.vertx

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.modules.ReadDbModule
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.modules.WriteDbModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.jdbc.JDBCClient
import javax.inject.Singleton


@Module(includes = [(WriteDbModule::class), (ReadDbModule::class)])
open class CrabzillaWebModule(val vertx: Vertx, val config: JsonObject) {

  @Provides
  @Singleton
  fun healthcheck(@WriteDatabase jdbcClientWrite: JDBCClient, @ReadDatabase jdbcClientRead: JDBCClient)
    : HealthCheckHandler {

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
