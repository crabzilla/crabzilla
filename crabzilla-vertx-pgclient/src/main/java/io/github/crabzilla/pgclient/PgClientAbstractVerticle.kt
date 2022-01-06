package io.github.crabzilla.pgclient

import io.github.crabzilla.pgclient.PgClientFactory.createPoolOptions
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber

open class PgClientAbstractVerticle : AbstractVerticle() {

  val pgPool: PgPool by lazy {
    val configId = config().getString("connectOptionsName")
    val connectOptions = PgClientFactory.createPgConnectOptions(config().getJsonObject(configId))
    val poolOptions = createPoolOptions(config().getJsonObject(configId))
    PgPool.pool(vertx, connectOptions, poolOptions)
  }

  val subscriber: PgSubscriber by lazy {
    val configId = config().getString("connectOptionsName")
    val connectOptions = PgClientFactory.createPgConnectOptions(config().getJsonObject(configId))
    PgSubscriber.subscriber(vertx, connectOptions)
  }
}
