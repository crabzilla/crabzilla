package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.EventsProjectorPublisher
import io.github.crabzilla.projection.ProjectorConfig
import io.vertx.core.Promise

class EventsProjectorVerticle : PgClientAbstractVerticle() {

  private lateinit var eventsProjectorPublisher: EventsProjectorPublisher

  override fun start(startPromise: Promise<Void>) {

    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    eventsProjectorPublisher = EventsProjectorPublisher(
      vertx, pgPool,
      subscriber, ProjectorConfig.create(config()), provider.create()
    )
    eventsProjectorPublisher.start(startPromise)
  }

  override fun stop() {
    eventsProjectorPublisher.stop()
  }
}
