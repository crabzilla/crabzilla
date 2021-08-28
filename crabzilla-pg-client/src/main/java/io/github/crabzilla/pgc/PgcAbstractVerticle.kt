package io.github.crabzilla.pgc

import io.github.crabzilla.core.serder.SerDer
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

open class PgcAbstractVerticle : AbstractVerticle() {

  fun serDer(config: JsonObject): SerDer {
    val provider = JsonContextProviderFinder().create(config.getString("jsonFactoryClassName"))
    return provider!!.create().serder()
  }

  fun pgPool(config: JsonObject): PgPool {
    val configId = config.getString("connectOptionsName")
    val connectOptions = createConnectOptions(config.getJsonObject(configId))
    val poolOptions = createPoolOptions(config.getJsonObject("poolOptions"))
    return PgPool.pool(vertx, connectOptions, poolOptions)
  }

  fun sqlClient(config: JsonObject): SqlClient {
    println(config.encodePrettily())
    val configId = config.getString("connectOptionsName")
    val connectOptions = createConnectOptions(config.getJsonObject(configId))
    val poolOptions = createPoolOptions(config.getJsonObject("poolOptions"))
    return PgPool.client(vertx, connectOptions, poolOptions)
  }

  private fun createConnectOptions(config: JsonObject): PgConnectOptions? {
    val pgConnectOptions = PgConnectOptions()
    val host = config.getString("host")
    pgConnectOptions.host = host
    val port = config.getInteger("port")
    pgConnectOptions.port = port
    val username = config.getString("user")
    pgConnectOptions.user = username
    val password = config.getString("password")
    pgConnectOptions.password = password
    val database = config.getString("database")
    pgConnectOptions.database = database
    // TODO config it
    pgConnectOptions.cachePreparedStatements = true
    pgConnectOptions.reconnectAttempts = 2
    pgConnectOptions.reconnectInterval = 1000
    return pgConnectOptions
  }

  private fun createPoolOptions(config: JsonObject): PoolOptions {
    return PoolOptions().setMaxSize(config.getInteger("maxSize", 10))
  }
}
