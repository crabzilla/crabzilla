package io.github.crabzilla.pgc

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions

fun getConfig(vertx: Vertx): Future<JsonObject> {
  val envOptions = ConfigStoreOptions()
    .setType("file")
    .setFormat("properties")
    .setConfig(JsonObject().put("path", "../example1.env"))
  val options = ConfigRetrieverOptions().addStore(envOptions)
  val retriever = ConfigRetriever.create(vertx, options)
  return retriever.config
}

fun getPgPool(vertx: Vertx, config: JsonObject): PgPool {
  return pgPool(vertx, config)
}

fun pgConnectionOptions(vertx: Vertx, config: JsonObject): PgConnectOptions {
  return PgConnectOptions()
    .setPort(config.getInteger("DATABASE_PORT"))
    .setHost(config.getString("DATABASE_HOST"))
    .setDatabase(config.getString("DATABASE_NAME"))
    .setUser(config.getString("DATABASE_USER"))
    .setPassword(config.getString("DATABASE_PASSWORD"))
}

fun pgPool(vertx: Vertx, config: JsonObject): PgPool {
  val pgPoolOptions = PoolOptions().setMaxSize(config.getInteger("DATABASE_POOL_MAX_SIZE"))
  return PgPool.pool(vertx, pgConnectionOptions(vertx, config), pgPoolOptions)
}

fun cleanDatabase(vertx: Vertx, config: JsonObject): Future<Void> {
  val promise = Promise.promise<Void>()
  val pgPool = getPgPool(vertx, config)
  pgPool.query("delete from commands").execute()
    .compose { pgPool.query("delete from events").execute() }
    .compose { pgPool.query("delete from snapshots").execute() }
    .compose { pgPool.query("update projections set last_offset = 0").execute() }
    .compose { pgPool.query("delete from customer_summary").execute() }
    .onSuccess {
      println("** Database is now clean")
      promise.complete()
    }.onFailure { promise.fail(it) }
  return promise.future()
}
