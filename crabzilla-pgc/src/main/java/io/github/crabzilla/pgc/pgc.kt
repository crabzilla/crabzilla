package io.github.crabzilla.pgc

import io.reactiverse.pgclient.*
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

fun whoIsRunningProjection(projectionEndpoint: String) : String {
  return "$projectionEndpoint-ping"
}

fun writeModelPgPool(vertx: Vertx, config: JsonObject) : PgPool {
  val id = "WRITE"
  val writeOptions = PgPoolOptions()
    .setPort(config.getInteger("${id}_DATABASE_PORT"))
    .setHost(config.getString("${id}_DATABASE_HOST"))
    .setDatabase(config.getString("${id}_DATABASE_NAME"))
    .setUser(config.getString("${id}_DATABASE_USER"))
    .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
    .setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
  return PgClient.pool(vertx, writeOptions)
}

fun readModelPgPool(vertx: Vertx, config: JsonObject) : PgPool {
  val id = "READ"
  val writeOptions = PgPoolOptions()
    .setPort(config.getInteger("${id}_DATABASE_PORT"))
    .setHost(config.getString("${id}_DATABASE_HOST"))
    .setDatabase(config.getString("${id}_DATABASE_NAME"))
    .setUser(config.getString("${id}_DATABASE_USER"))
    .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
    .setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
  return PgClient.pool(vertx, writeOptions)
}

/**
 * https://dzone.com/articles/three-paradigms-of-asynchronous-programming-in-ver
 */
fun PgTransaction.runPreparedQuery(query: String, tuple: Tuple, future: Future<Void>) {
  this.preparedQuery(query, tuple) { ar2 ->
    //    println("running $query with $tuple")
    if (ar2.failed()) {
//      println("    failed ${ar2.cause()}" )
      future.fail(ar2.cause())
    } else {
      future.complete()
    }
  }
}
