package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.EventsProjectorWorker
import io.github.crabzilla.projection.ProjectorConfig
import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.vertx.core.Promise

class EventsProjectorVerticle : PgClientAbstractVerticle() {

  private lateinit var eventsProjectorWorker: EventsProjectorWorker

  override fun start(startPromise: Promise<Void>) {
    val config = ProjectorConfig.create(config())
    val pgEventProjector = when (config.projectorStrategy) {
      POSTGRES_SAME_TRANSACTION -> {
        val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
        provider.create()
      } else -> {
        null
      }
    }
    eventsProjectorWorker = EventsProjectorWorker(vertx, pgPool, subscriber, config, pgEventProjector)
    eventsProjectorWorker.start(startPromise)
  }

  override fun stop() {
    eventsProjectorWorker.stop()
  }
}
