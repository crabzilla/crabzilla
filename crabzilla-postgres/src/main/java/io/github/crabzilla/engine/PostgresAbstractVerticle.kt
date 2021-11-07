package io.github.crabzilla.engine

import io.github.crabzilla.core.json.JsonSerDer
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

open class PostgresAbstractVerticle : AbstractVerticle() {

  val jsonSerDer: JsonSerDer by lazy {
    val provider = JsonContextProviderFinder().create(config().getString("jsonFactoryClassName"))
    provider.create().get()
  }

  val pgPool: PgPool by lazy {
    val configId = config().getString("connectOptionsName")
    val connectOptions = createConnectOptions(config().getJsonObject(configId))
    val poolOptions = createPoolOptions(config().getJsonObject("poolOptions"))
    PgPool.pool(vertx, connectOptions, poolOptions)
  }

  val sqlClient: SqlClient by lazy {
    val configId = config().getString("connectOptionsName")
    val connectOptions = createConnectOptions(config().getJsonObject(configId))
    val poolOptions = createPoolOptions(config().getJsonObject("poolOptions"))
    PgPool.client(vertx, connectOptions, poolOptions)
  }

  val connectOptions: PgConnectOptions by lazy {
    val pgConnectOptions = PgConnectOptions()
    val host = config().getString("host")
    pgConnectOptions.host = host
    val port = config().getInteger("port")
    pgConnectOptions.port = port
    val username = config().getString("user")
    pgConnectOptions.user = username
    val password = config().getString("password")
    pgConnectOptions.password = password
    val database = config().getString("database")
    pgConnectOptions.database = database
    // TODO config it
    pgConnectOptions.cachePreparedStatements = true
    pgConnectOptions.reconnectAttempts = 2
    pgConnectOptions.reconnectInterval = 1000
    pgConnectOptions
  }

  private fun createConnectOptions(config: JsonObject): PgConnectOptions {
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
