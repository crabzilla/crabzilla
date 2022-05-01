package io.github.crabzilla.projection

import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions

open class PgClientAbstractVerticle : AbstractVerticle() {

  val pgPool: PgPool by lazy {
    val pgConfig = PgConnectOptionsFactory.from(config())
    val connectOptions = PgConnectOptionsFactory.from(pgConfig)
    PgPool.pool(vertx, connectOptions, PoolOptions())
  }

  val subscriber: PgSubscriber by lazy {
    val pgConfig = PgConnectOptionsFactory.from(config())
    val connectOptions = PgConnectOptionsFactory.from(pgConfig)
    PgSubscriber.subscriber(vertx, connectOptions)
  }
}
