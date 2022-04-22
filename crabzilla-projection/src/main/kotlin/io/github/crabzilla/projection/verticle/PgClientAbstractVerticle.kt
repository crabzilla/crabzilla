package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.PgConnectOptionsFactory
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

open class PgClientAbstractVerticle : AbstractVerticle() {

  val sqlClint: SqlClient by lazy {
    val pgConfig = PgConnectOptionsFactory.from(config())
    val connectOptions = PgConnectOptionsFactory.from(pgConfig)
    PgPool.client(vertx, connectOptions, PoolOptions())
  }

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
