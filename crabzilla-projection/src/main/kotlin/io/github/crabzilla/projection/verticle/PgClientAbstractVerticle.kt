package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.PgConnectOptionsFactory
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions

open class PgClientAbstractVerticle : AbstractVerticle() {

  val pgPool: PgPool by lazy {
    val connectOptions = PgConnectOptionsFactory.from(config())
    PgPool.pool(vertx, connectOptions, PoolOptions())
  }

  val subscriber: PgSubscriber by lazy {
    val connectOptions = PgConnectOptionsFactory.from(config())
    PgSubscriber.subscriber(vertx, connectOptions)
  }
}
