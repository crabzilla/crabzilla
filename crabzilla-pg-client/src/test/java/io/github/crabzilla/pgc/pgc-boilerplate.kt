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
import io.vertx.pgclient.pubsub.PgSubscriber
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
  write.query("delete from commands").execute()
    .compose { write.query("delete from events").execute() }
    .compose { write.query("delete from customer_snapshots").execute() }
    .compose { write.query("update projections set last_offset = 0").execute() }
    .compose { read.query("delete from customer_summary").execute() }
    .onSuccess {
      println("** Database is now clean")
      promise.complete()
    }.onFailure { promise.fail(it) }
  return promise.future()
}
