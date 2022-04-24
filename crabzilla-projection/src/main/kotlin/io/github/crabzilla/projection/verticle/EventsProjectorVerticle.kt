package io.github.crabzilla.projection.verticle

import io.github.crabzilla.projection.EventsProjectorComponent
import io.github.crabzilla.projection.ProjectorConfig
import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.github.crabzilla.stack.projection.PgEventProjector
import io.vertx.core.Promise

class EventsProjectorVerticle : PgClientAbstractVerticle() {

  private lateinit var eventsProjectorComponent: EventsProjectorComponent

  override fun start(startPromise: Promise<Void>) {

    val config = ProjectorConfig.create(config())

    var pgEventProjector: PgEventProjector? = null
    if (config.projectorStrategy == POSTGRES_SAME_TRANSACTION) {
      val provider = EventProjectorProviderFactory().create(config().getString("eventsProjectorFactoryClassName"))
      pgEventProjector = provider.create()
    }

    eventsProjectorComponent = EventsProjectorComponent(vertx, pgPool, subscriber, config, pgEventProjector)
    eventsProjectorComponent.start()
      .onSuccess { startPromise.complete() }
      .onFailure { startPromise.fail(it) }
  }

  override fun stop() {
    eventsProjectorComponent.stop()
  }
}
