package io.github.crabzilla

import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import java.net.URI

object PgConnectOptionsFactory {

  fun from(json: JsonObject): PgConnectOptions {
    val uri = URI.create(json.getString("url"))
    val options = PgConnectOptions()
    options.host = uri.host
    options.port = uri.port
    options.database = uri.path.replace("/", "")
    options.user = json.getString("username")
    options.password = json.getString("password")
    return options
  }

  fun from(pgConfig: PgConfig): PgConnectOptions {
    val uri = URI.create(pgConfig.url())
    val options = PgConnectOptions()
    options.host = uri.host
    options.port = uri.port
    options.database = uri.path.replace("/", "")
    options.user = pgConfig.username()
    options.password = pgConfig.password()
    return options
  }
}
