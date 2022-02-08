package io.github.crabzilla.pgclient

import io.github.crabzilla.pgclient.PgClientFactory.createPoolOptions
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber

open class PgClientAbstractVerticle : AbstractVerticle() {

  val pgPool: PgPool by lazy {
    val connectOptions = PgClientFactory.createPgConnectOptions(config())
    val poolOptions = createPoolOptions(config())
    PgPool.pool(vertx, connectOptions, poolOptions)
  }

  val subscriber: PgSubscriber by lazy {
    val connectOptions = PgClientFactory.createPgConnectOptions(config())
    PgSubscriber.subscriber(vertx, connectOptions)
  }
}
