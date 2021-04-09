package io.github.crabzilla.pgc

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet

fun getConfig(vertx: Vertx): Future<JsonObject> {
  val envOptions = ConfigStoreOptions()
    .setType("file")
    .setFormat("properties")
    .setConfig(JsonObject().put("path", "../example1.env"))
  val options = ConfigRetrieverOptions().addStore(envOptions)
  val retriever = ConfigRetriever.create(vertx, options)
  return retriever.config
}

fun writeModelPgPool(vertx: Vertx, config: JsonObject): PgPool {
  return pgPool(vertx, config, "WRITE")
}

fun readModelPgPool(vertx: Vertx, config: JsonObject): PgPool {
  return pgPool(vertx, config, "READ")
}

fun pgSubscriber(vertx: Vertx, config: JsonObject): PgSubscriber {
  return PgSubscriber.subscriber(vertx, pgConnectionOptions(vertx, config, "WRITE"))
}

fun pgConnectionOptions(vertx: Vertx, config: JsonObject, id: String): PgConnectOptions {
  return PgConnectOptions()
    .setPort(config.getInteger("${id}_DATABASE_PORT"))
    .setHost(config.getString("${id}_DATABASE_HOST"))
    .setDatabase(config.getString("${id}_DATABASE_NAME"))
    .setUser(config.getString("${id}_DATABASE_USER"))
    .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
}

fun pgPool(vertx: Vertx, config: JsonObject, id: String): PgPool {
  val pgPoolOptions = PoolOptions().setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
  return PgPool.pool(vertx, pgConnectionOptions(vertx, config, id), pgPoolOptions)
}

fun cleanDatabase(vertx: Vertx, config: JsonObject): Future<Void> {
  val promise = Promise.promise<Void>()
  val read = readModelPgPool(vertx, config)
  val write = writeModelPgPool(vertx, config)
  write.query("delete from events").execute { event1: AsyncResult<RowSet<Row?>?> ->
    if (event1.failed()) {
      promise.fail(event1.cause())
      return@execute
    }
    write.query("delete from commands").execute { event2: AsyncResult<RowSet<Row?>?> ->
      if (event2.failed()) {
        promise.fail(event2.cause())
        return@execute
      }
      write.query("delete from customer_snapshots").execute { event3: AsyncResult<RowSet<Row?>?> ->
        if (event3.failed()) {
          promise.fail(event3.cause())
          return@execute
        }
        read.query("delete from customer_summary").execute { event4: AsyncResult<RowSet<Row?>?> ->
          if (event4.failed()) {
            promise.fail(event4.cause())
            return@execute
          }
          write.query("update projections set last_offset = 0")
            .execute { event5: AsyncResult<RowSet<Row?>?> ->
              if (event5.failed()) {
                promise.fail(event5.cause())
                return@execute
              }
              println("*** database is now clean")
              promise.complete()
            }
        }
      }
    }
  }
  return promise.future()
}
