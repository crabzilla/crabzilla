package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.EventsProjector
import io.github.crabzilla.projection.EventsProjectorPublisher
import io.github.crabzilla.projection.ProjectorConfig
import io.vertx.core.Promise

class EventsProjectorVerticle : PgClientAbstractVerticle() {

  private lateinit var eventsProjectorPublisher: EventsProjectorPublisher
  private lateinit var eventsProjector: EventsProjector

  override fun start(startPromise: Promise<Void>) {

    val provider = EventsProjectorProviderFinder().create(config().getString("eventsProjectorFactoryClassName"))
    eventsProjector = provider.create()
    eventsProjectorPublisher = EventsProjectorPublisher(
      vertx, pgPool, subscriber,
      ProjectorConfig.create(config()), eventsProjector
    )
    eventsProjectorPublisher.start(startPromise)
  }

  override fun stop() {
    eventsProjectorPublisher.stop()
  }
}
