package io.github.crabzilla.pgc

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

class PgcComponent(val vertx: Vertx, val config: JsonObject) {
  val readDb = readModelPgPool(vertx, config)
  val writeDb = writeModelPgPool(vertx, config)
  val projectionEndpoint: String = config.getString("PROJECTION_ENDPOINT")
}
