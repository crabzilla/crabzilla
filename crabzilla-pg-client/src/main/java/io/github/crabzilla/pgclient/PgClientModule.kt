package io.github.crabzilla.pgclient

import dagger.Module
import dagger.Provides
import io.github.crabzilla.vertx.ReadDatabase
import io.github.crabzilla.vertx.WriteDatabase
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import javax.inject.Singleton

@Module
open class PgClientModule {

  @Provides
  @Singleton
  @WriteDatabase
  fun pgPool1(config: JsonObject, vertx: Vertx): PgPool {
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
  fun pgPool2(config: JsonObject, vertx: Vertx): PgPool {
    val options = PgPoolOptions()
      .setPort(5432)
      .setHost(config.getString("READ_DATABASE_HOST"))
      .setDatabase(config.getString("READ_DATABASE_NAME"))
      .setUser(config.getString("READ_DATABASE_USER"))
      .setPassword(config.getString("READ_DATABASE_PASSWORD"))
      .setMaxSize(config.getInteger("READ_DATABASE_POOL_MAX_SIZE"))
    return PgClient.pool(vertx, options)
  }

}
