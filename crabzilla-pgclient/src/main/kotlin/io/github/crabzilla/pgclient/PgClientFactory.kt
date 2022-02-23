package io.github.crabzilla.pgclient

import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.PoolOptions

object PgClientFactory {

  const val DB_CONFIG_TAG: String = "targetDatabase"

  fun createPgConnectOptions(config: JsonObject): PgConnectOptions {
    val targetDb = config.getString(DB_CONFIG_TAG)
    val sqlConfig = config.getJsonObject(targetDb)?: config
    val pgConnectOptions = PgConnectOptions()
    val host: String = sqlConfig.getString("host")
    val port: Int = sqlConfig.getInteger("port")
    val username: String = sqlConfig.getString("user")
    val password: String = sqlConfig.getString("password")
    val database: String = sqlConfig.getString("database")
    pgConnectOptions.host = host
    pgConnectOptions.port = port
    pgConnectOptions.user = username
    pgConnectOptions.password = password
    pgConnectOptions.database = database
    return pgConnectOptions
  }

  fun createPoolOptions(config: JsonObject): PoolOptions {
    val targetDb = config.getString(DB_CONFIG_TAG)
    val sqlConfig = config.getJsonObject(targetDb)?: config
    val poolOptions = PoolOptions()
    val poolConfig: JsonObject = sqlConfig.getJsonObject("pool") ?: return poolOptions
    if (poolConfig.containsKey("maxSize")) {
      poolOptions.maxSize = poolConfig.getInteger("maxSize")
    }
    return poolOptions
  }
}
