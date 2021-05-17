package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventBusPublisher
import io.github.crabzilla.stack.EventsPublisherVerticle
import io.github.crabzilla.stack.EventsPublisherVerticleOptions
import io.vertx.pgclient.PgPool

object EventsPublisherVerticleFactory {

  fun create(projection: String, writeDb: PgPool, options: EventsPublisherVerticleOptions): EventsPublisherVerticle {
    val eventsScanner = PgcEventsScanner(writeDb, projection) // consider an additional optional param to filter events
    return EventsPublisherVerticle(eventsScanner, EventBusPublisher(options.targetEndpoint, options.eventBus), options)
  }
}
