package io.github.crabzilla.projection

import io.github.crabzilla.projection.PgClientFactory.createPoolOptions
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
