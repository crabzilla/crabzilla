package io.github.crabzilla.stack

import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.stack.command.CommandServiceOptions
import io.github.crabzilla.stack.command.internal.CommandService
import io.github.crabzilla.stack.command.CommandServiceApi
import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.github.crabzilla.stack.subscription.SubscriptionConfig
import io.github.crabzilla.stack.subscription.internal.SubscriptionComponent
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.PoolOptions
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray

class CrabzillaVertxContext constructor(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val pgConfig: JsonObject) : CrabzillaContext {

  private val subscriptionVerticles: MutableList<AbstractVerticle> = mutableListOf()
  // TODO status (initial, deployed)

  companion object {

    private val log = LoggerFactory.getLogger(CrabzillaVertxContext::class.java)

    fun new(vertx: Vertx, pgConfig: JsonObject): CrabzillaVertxContext {
      log.info("Creating without pgPool")
      return CrabzillaVertxContext(vertx, toPgPool(vertx, toPgConnectionOptions(pgConfig)), pgConfig)
    }

//    fun new(vertx: Vertx, pgPool: PgPool, pgConfig: JsonObject): CrabzillaContext {
//      log.info("Creating with pgPool")
//      return CrabzillaContext(vertx, pgPool, pgConfig)
//    }

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

  override fun vertx(): Vertx = vertx
  override fun pgPool(): PgPool = pgPool

  override fun deploy(subscriptionOptions: DeploymentOptions): Future<Void> {
    val initialFuture = Future.succeededFuture<Void>()
    return subscriptionVerticles.fold(
        initialFuture
      ) { currentFuture: Future<Void>, v ->
        currentFuture.compose {
          vertx.deployVerticle(v, subscriptionOptions).mapEmpty()
        }
    }.onSuccess {
      log.info("Verticles deployed: ${subscriptionVerticles.size} ")
    }
  }

  override fun <S : Any, C : Any, E : Any> commandService(
    component: FeatureComponent<S, C, E>,
    jsonObjectSerDer: JsonObjectSerDer<S, C, E>,
    options: CommandServiceOptions,
  ): CommandServiceApi<C> {
    return CommandService(vertx, pgPool, component, jsonObjectSerDer, options)
  }

  override fun subscription(config: SubscriptionConfig, eventProjector: EventProjector?)
  : SubscriptionApi {
    log.info("Creating subscription")
    val verticle = object : AbstractVerticle() {
      private lateinit var subscription: SubscriptionComponent
      override fun start(promise: Promise<Void>) {
        subscription = SubscriptionComponent(vertx, pgPool, pgSubscriber(), config, eventProjector)
        subscription.start()
          .onSuccess {
            log.info("Subscription [${config.subscriptionName}] started")
            promise.complete()
          }
          .onFailure { promise.fail(it) }
      }
    }
    subscriptionVerticles.add(verticle)
    return SubscriptionApi(vertx.eventBus(), config.subscriptionName)
  }

  override fun pgSubscriber(): PgSubscriber {
    log.info("Creating postgres subscriber")
    return PgSubscriber.subscriber(vertx, toPgConnectionOptions(pgConfig))
  }

}
