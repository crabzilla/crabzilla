package io.github.crabzilla.pgc

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet

fun writeModelPgPool(vertx: Vertx, config: JsonObject): PgPool {
  return pgPool(vertx, config, "WRITE")
}

fun readModelPgPool(vertx: Vertx, config: JsonObject): PgPool {
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
  return PgPool.pool(vertx, readOptions, pgPoolOptions)
}

fun cleanDatabase(vertx: Vertx, config: JsonObject): Future<Void> {
  val promise = Promise.promise<Void>()
  val read = readModelPgPool(vertx, config)
  val write = writeModelPgPool(vertx, config)
  write.query("delete from units_of_work").execute { event1: AsyncResult<RowSet<Row?>?> ->
    if (event1.failed()) {
      promise.fail(event1.cause())
      return@execute
    }
    write.query("delete from customer_snapshots").execute { event2: AsyncResult<RowSet<Row?>?> ->
      if (event2.failed()) {
        promise.fail(event2.cause())
        return@execute
      }
      read.query("delete from projections").execute { event3: AsyncResult<RowSet<Row?>?> ->
        if (event3.failed()) {
          promise.fail(event3.cause())
          return@execute
        }
        read.query("delete from customer_summary").execute { event3: AsyncResult<RowSet<Row?>?> ->
          if (event3.failed()) {
            promise.fail(event3.cause())
            return@execute
          }
          promise.complete()
        }
      }
    }
  }
  return promise.future()
}
