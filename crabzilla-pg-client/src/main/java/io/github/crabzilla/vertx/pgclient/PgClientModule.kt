package io.github.crabzilla.vertx.pgclient

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.github.crabzilla.vertx.initVertx
import io.github.crabzilla.vertx.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.healthchecks.Status
import javax.inject.Singleton

@Module
open class PgClientModule(val vertx: Vertx, val config: JsonObject) {

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
  @WriteDatabase
  fun pgPool1(config: JsonObject): PgPool {
    val options = PgPoolOptions()
      .setPort(5432)
      .setHost(config.getString("WRITE_DATABASE_HOST"))
      .setDatabase(config.getString("WRITE_DATABASE_NAME"))
      .setUser(config.getString("WRITE_DATABASE_USER"))
      .setPassword(config.getString("WRITE_DATABASE_PASSWORD"))
      .setMaxSize(config.getInteger("WRITE_DATABASE_POOL_MAX_SIZE"))
    return PgClient.pool(vertx, options)
  }

  @Provides
  @Singleton
  @ReadDatabase
  fun pgPool2(config: JsonObject): PgPool {
    val options = PgPoolOptions()
      .setPort(5432)
      .setHost(config.getString("READ_DATABASE_HOST"))
      .setDatabase(config.getString("READ_DATABASE_NAME"))
      .setUser(config.getString("READ_DATABASE_USER"))
      .setPassword(config.getString("READ_DATABASE_PASSWORD"))
      .setMaxSize(config.getInteger("READ_DATABASE_POOL_MAX_SIZE"))
    return PgClient.pool(vertx, options)
  }

  @Provides
  @IntoMap
  @StringKey("read-database")
  fun healthcheck1(@ReadDatabase readPool: PgPool) : Handler<Future<Status>> {
    return Handler { future: Future<Status> ->
        readPool.query("select 1", { ar ->
        if (ar.succeeded()) {
          future.succeeded()
        } else {
          future.fail(ar.cause())
        }
        readPool.close()
      })
    }
  }

  @Provides
  @IntoMap
  @StringKey("write-database")
  fun healthcheck2(@WriteDatabase writePool: PgPool) : Handler<Future<Status>> {
    return Handler { future: Future<Status> ->
      writePool.query("select 1", { ar ->
        if (ar.succeeded()) {
          future.succeeded()
        } else {
          future.fail(ar.cause())
        }
        writePool.close()
      })
    }
  }

}
