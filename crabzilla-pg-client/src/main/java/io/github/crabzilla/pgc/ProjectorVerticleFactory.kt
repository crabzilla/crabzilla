package io.github.crabzilla.pgc

import io.github.crabzilla.stack.EventBusPublisher
import io.github.crabzilla.stack.PoolingProjectionVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.pgclient.PgPool

object ProjectorVerticleFactory {

  fun create(
    projection: String,
    targetTopic: String,
    eventBus: EventBus,
    writeDb: PgPool,
    interval: Long = 500,
    rows: Int = 500
  ): PoolingProjectionVerticle {
    val eventsScanner = PgcEventsScanner(writeDb, projection)
    return PoolingProjectionVerticle(eventsScanner, EventBusPublisher(targetTopic, eventBus), interval, rows)
  }
}
