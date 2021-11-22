package io.github.crabzilla.projection

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.projection.internal.OptionsFactory
import io.github.crabzilla.projection.internal.OptionsFactory.createPoolOptions
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.SqlClient

open class PostgresAbstractVerticle : AbstractVerticle() {

  val jsonSerDer: JsonSerDer by lazy {
    val provider = JsonContextProviderFinder().create(config().getString("jsonFactoryClassName"))
    provider.create().get()
  }

  val pgPool: PgPool by lazy {
    val configId = config().getString("connectOptionsName")
    val connectOptions = OptionsFactory.createPgConnectOptions(config().getJsonObject(configId))
    val poolOptions = createPoolOptions(config().getJsonObject(configId))
    PgPool.pool(vertx, connectOptions, poolOptions)
  }

  val sqlClient: SqlClient by lazy {

    val configId = config().getString("connectOptionsName")
    val connectOptions = OptionsFactory.createPgConnectOptions(config().getJsonObject(configId))
    val poolOptions = createPoolOptions(config().getJsonObject(configId))
    PgPool.client(vertx, connectOptions, poolOptions)
  }

  val subscriber: PgSubscriber by lazy {
    val configId = config().getString("connectOptionsName")
    val connectOptions = OptionsFactory.createPgConnectOptions(config().getJsonObject(configId))
    PgSubscriber.subscriber(vertx, connectOptions)
  }
}
