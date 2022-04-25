package io.github.crabzilla.projection

import io.github.crabzilla.projection.internal.EventsProjectorComponent
import io.github.crabzilla.stack.PgConfig
import io.github.crabzilla.stack.PgConnectOptionsFactory
import io.github.crabzilla.stack.projection.PgEventProjector
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.pubsub.PgSubscriber

class EventsProjectorFactory(val pgPool: PgPool, val pgConfig: PgConfig) {

  // TODO what about a container: registering verticles then deploy then all, (and having the deploymentId) ?
//  fun deployAll() : Future<Void> {
//    return Future.succeededFuture()
//  }

  fun createVerticle(config: ProjectorConfig, pgEventProjector: PgEventProjector? = null): AbstractVerticle {
    return object : AbstractVerticle() {
      lateinit var eventsProjectorComponent: EventsProjectorComponent
      override fun start(startPromise: Promise<Void>) {
        val pgPoolOptions: PgConnectOptions = PgConnectOptionsFactory.from(pgConfig)
        val pgSubscriber = PgSubscriber.subscriber(vertx, pgPoolOptions)
        eventsProjectorComponent = EventsProjectorComponent(vertx, pgPool, pgSubscriber, config, pgEventProjector)
        eventsProjectorComponent.start()
          .onSuccess { startPromise.complete() }
          .onFailure { startPromise.fail(it) }
      }
      override fun stop() {
        eventsProjectorComponent.stop()
      }
    }
  }
}
