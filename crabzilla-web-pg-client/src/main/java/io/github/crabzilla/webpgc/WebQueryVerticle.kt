package io.github.crabzilla.webpgc

import io.github.crabzilla.pgc.readModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool

abstract class WebQueryVerticle : AbstractVerticle() {
  val readDb: PgPool by lazy { readModelPgPool(vertx, config()) }
  val httpPort: Int by lazy { config().getInteger("READ_HTTP_PORT") }
}
