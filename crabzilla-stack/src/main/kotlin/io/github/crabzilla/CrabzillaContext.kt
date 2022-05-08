package io.github.crabzilla

import io.github.crabzilla.command.FeatureController
import io.github.crabzilla.command.FeatureOptions
import io.github.crabzilla.core.FeatureComponent
import io.github.crabzilla.projection.ProjectorApi
import io.github.crabzilla.projection.ProjectorConfig
import io.github.crabzilla.projection.internal.EventsProjectorComponent
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

  open fun <S : Any, C : Any, E : Any> featureController(
    component: FeatureComponent<S, C, E>,
    jsonObjectSerDer: JsonObjectSerDer<S, C, E>,
    options: FeatureOptions = FeatureOptions(),
  ): FeatureController<S, C, E> {
    return FeatureController(vertx, pgPool, component, jsonObjectSerDer, options)
  }

  open fun postgresProjector(config: ProjectorConfig, eventProjector: EventProjector)
  : Pair<AbstractVerticle, ProjectorApi> {
    log.info("Creating postgres projector")
    val verticle = object : AbstractVerticle() {
      private lateinit var projector: EventsProjectorComponent
      override fun start(promise: Promise<Void>) {
        projector = EventsProjectorComponent(vertx, pgPool, pgSubscriber(), config, eventProjector)
        projector.start()
          .onSuccess { promise.complete() }
          .onFailure { promise.fail(it) }
      }
    }
    return Pair(verticle, ProjectorApi(vertx.eventBus(), config.projectionName))
  }

  open fun eventBusProjector(config: ProjectorConfig): AbstractVerticle {
    log.info("Creating eventBus projector")
    return object : AbstractVerticle() {
      private lateinit var projector: EventsProjectorComponent
      override fun start(promise: Promise<Void>) {
        projector = EventsProjectorComponent(vertx, pgPool, pgSubscriber(), config, null)
        projector.start()
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
