package io.github.crabzilla.pgc

import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("Pgc")

fun writeModelPgPool(vertx: Vertx, config: JsonObject) : PgPool {
  return pgPool(vertx, config, "WRITE")
}

fun readModelPgPool(vertx: Vertx, config: JsonObject) : PgPool {
  return pgPool(vertx, config, "READ")
}

fun pgPool(vertx: Vertx, config: JsonObject, id: String): PgPool {
  val readOptions = PgConnectOptions()
    .setPort(config.getInteger("${id}_DATABASE_PORT"))
    .setHost(config.getString("${id}_DATABASE_HOST"))
    .setDatabase(config.getString("${id}_DATABASE_NAME"))
    .setUser(config.getString("${id}_DATABASE_USER"))
    .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
  val pgPoolOptions = PoolOptions().setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
  return PgPool.pool(vertx, readOptions,  pgPoolOptions)
}

fun Transaction.runPreparedQuery(query: String, tuple: Tuple) : Promise<Void> {
  val promise = Promise.promise<Void>()
  this.preparedQuery(query)
    .execute(tuple) { event ->
    if (event.failed()) {
      promise.fail(event.cause())
    } else {
      promise.complete()
    }
  }
  return promise
}

