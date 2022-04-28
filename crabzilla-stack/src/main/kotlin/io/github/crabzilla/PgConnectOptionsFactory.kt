package io.github.crabzilla

import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import java.net.URI

object PgConnectOptionsFactory {

  fun from(json: JsonObject): PgConfig {
    val options = object : PgConfig {
      override fun username(): String {
        return json.getString("username")
      }
      override fun password(): String {
        return json.getString("password")
      }
      override fun url(): String {
        return json.getString("url")
      }
    }
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
