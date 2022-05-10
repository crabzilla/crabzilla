package io.github.crabzilla

import io.github.crabzilla.command.FeatureService
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.subscription.SubscriptionApi
import io.github.crabzilla.subscription.SubscriptionConfig
import io.github.crabzilla.subscription.internal.SubscriptionComponent
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions
import org.slf4j.LoggerFactory
import java.net.URI

open class CrabzillaContext private constructor(val vertx: Vertx, val pgPool: PgPool,
                                                private val pgConfig: JsonObject) {

  companion object {

    private val log = LoggerFactory.getLogger(CrabzillaContext::class.java)

    const val POSTGRES_NOTIFICATION_CHANNEL = "crabzilla_channel"
    const val EVENTBUS_GLOBAL_TOPIC = "crabzilla.eventbus.global-topic"

    fun new(vertx: Vertx, pgConfig: JsonObject): CrabzillaContext {
      log.info("Creating without pgPool")
      return CrabzillaContext(vertx, toPgPool(vertx, toPgConnectionOptions(pgConfig)), pgConfig)
    }

    fun new(vertx: Vertx, pgPool: PgPool, pgConfig: JsonObject): CrabzillaContext {
      log.info("Creating with pgPool")
      return CrabzillaContext(vertx, pgPool, pgConfig)
    }

    private fun toPgConnectionOptions(pgConfig: JsonObject): PgConnectOptions {
      val options = PgConnectOptions()
      val uri = URI.create(pgConfig.getString("url"))
      options.host = uri.host
      options.port = uri.port
      options.database = uri.path.replace("/", "")
      options.user = pgConfig.getString("username")
      options.password = pgConfig.getString("password")
      return options
    }

    private fun toPgPool(vertx: Vertx, options: PgConnectOptions): PgPool {
      return PgPool.pool(vertx, options, PoolOptions())
    }

  }

  open fun <S : Any, C : Any, E : Any> featureService(
    component: FeatureComponent<S, C, E>,
    jsonObjectSerDer: JsonObjectSerDer<S, C, E>,
    options: FeatureOptions = FeatureOptions(),
  ): FeatureService<S, C, E> {
    return FeatureService(vertx, pgPool, component, jsonObjectSerDer, options)
  }

  open fun subscriptionWithPostgresSink(config: SubscriptionConfig, eventProjector: EventProjector)
  : Pair<AbstractVerticle, SubscriptionApi> {
    log.info("Creating subscription with postgres sink")
    val verticle = object : AbstractVerticle() {
      private lateinit var subscription: SubscriptionComponent
      override fun start(promise: Promise<Void>) {
        subscription = SubscriptionComponent(vertx, pgPool, pgSubscriber(), config, eventProjector)
        subscription.start()
          .onSuccess { promise.complete() }
          .onFailure { promise.fail(it) }
      }
    }
    return Pair(verticle, SubscriptionApi(vertx.eventBus(), config.subscriptionName))
  }

  open fun subscriptionWithEventBusSink(config: SubscriptionConfig): AbstractVerticle {
    log.info("Creating subscription with eventbus sink")
    return object : AbstractVerticle() {
      private lateinit var subscription: SubscriptionComponent
      override fun start(promise: Promise<Void>) {
        subscription = SubscriptionComponent(vertx, pgPool, pgSubscriber(), config, null)
        subscription.start()
          .onSuccess { promise.complete() }
          .onFailure { promise.fail(it) }
      }
    }
  }

  open fun pgSubscriber(): PgSubscriber {
    log.info("Creating postgres subscriber with ${pgConfig.encodePrettily()}")
    return PgSubscriber.subscriber(vertx, toPgConnectionOptions(pgConfig))
  }

}
