package io.github.crabzilla.projection

import io.github.crabzilla.EventProjector
import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.github.crabzilla.projection.internal.EventProjectorProviderFactory
import io.vertx.core.Promise

class EventsProjectorVerticle : PgClientAbstractVerticle() {

  private lateinit var eventsProjectorComponent: EventsProjectorComponent

  // TODO using EventsProjectorComponent from withing a verticle is the best. It should be internal (not a public API)
  // TODO for quarkus: another verticle receiving PgPool, PgSubscriber, ProjectorConfig and EventProjector?
  override fun start(startPromise: Promise<Void>) {

    val config = ProjectorConfig.create(config())

    var eventProjector: EventProjector? = null
    if (config.projectorStrategy == POSTGRES_SAME_TRANSACTION) {
      val provider = EventProjectorProviderFactory().create(config().getString("eventsProjectorFactoryClassName"))
      eventProjector = provider.create()
    }

    eventsProjectorComponent = EventsProjectorComponent(vertx, pgPool, subscriber, config, eventProjector)
    eventsProjectorComponent.start()
      .onSuccess { startPromise.complete() }
      .onFailure { startPromise.fail(it) }
  }

  override fun stop() {
    eventsProjectorComponent.stop()
  }
}
