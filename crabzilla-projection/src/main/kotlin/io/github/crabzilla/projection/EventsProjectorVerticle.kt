package io.github.crabzilla.projection

import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.github.crabzilla.projection.internal.EventProjectorProviderFactory
import io.github.crabzilla.projection.internal.EventsProjectorComponent
import io.github.crabzilla.stack.PgEventProjector
import io.vertx.core.Promise

class EventsProjectorVerticle : PgClientAbstractVerticle() {

  private lateinit var eventsProjectorComponent: EventsProjectorComponent

  // TODO using EventsProjectorComponent from withing a verticle is the best. It should be internal (not a public API)
  // TODO for quarkus: another verticle receiving PgPool, PgSubscriber, ProjectorConfig and PgEventProjector?
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
