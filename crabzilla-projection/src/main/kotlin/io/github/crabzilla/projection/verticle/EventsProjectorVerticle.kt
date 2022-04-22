package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.EventsProjectorWorker
import io.github.crabzilla.projection.ProjectorConfig
import io.vertx.core.Promise

class EventsProjectorVerticle : PgClientAbstractVerticle() {

  private lateinit var eventsProjectorWorker: EventsProjectorWorker

  override fun start(startPromise: Promise<Void>) {

    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    eventsProjectorWorker = EventsProjectorWorker(
      vertx, pgPool,
      subscriber, ProjectorConfig.create(config()), provider.create()
    )
    eventsProjectorWorker.start(startPromise)
  }

  override fun stop() {
    eventsProjectorWorker.stop()
  }
}
