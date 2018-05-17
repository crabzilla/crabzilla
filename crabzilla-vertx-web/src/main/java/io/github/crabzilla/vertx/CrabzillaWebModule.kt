package io.github.crabzilla.vertx

import dagger.Module
import dagger.Provides
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
  @WebHealthCheck
  fun healthcheck(@WriteDatabase jdbcClientWrite: JDBCClient, @ReadDatabase jdbcClientRead: JDBCClient)
    : HealthCheckHandler {

    val healthCheckHandler = HealthCheckHandler.create(vertx)

    healthCheckHandler.register("health-write-database",{ future ->
      println("*** healht check")
      jdbcClientWrite.getConnection({ connection ->
        if (connection.failed()) {
          println("*** healht check failed")
          connection.cause().printStackTrace()
          future.fail(connection.cause())
        } else {
          connection.result().close()
          future.complete(Status.OK())
        }
      })
    })

    healthCheckHandler.register("health-read-database",{ future ->
      jdbcClientRead.getConnection({ connection ->
        if (connection.failed()) {
          future.fail(connection.cause())
        } else {
          connection.result().close()
          future.complete(Status.OK())
        }
      })
    })

    // TODO
    healthCheckHandler.register("health-handler", { f -> f.complete(Status.OK()) })
    healthCheckHandler.register("health-projector", { f -> f.complete(Status.OK()) })

    return healthCheckHandler

  }

}
