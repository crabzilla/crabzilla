package io.github.crabzilla.stack1

import io.github.crabzilla.pgc.readModelPgPool
import io.github.crabzilla.pgc.writeModelPgPool
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

class Stack1Component(val vertx: Vertx, val config: JsonObject) {

  val readDb = readModelPgPool(vertx, config)
  val writeDb = writeModelPgPool(vertx, config)
  val projectionEndpoint: String = config.getString("PROJECTION_ENDPOINT")
}
