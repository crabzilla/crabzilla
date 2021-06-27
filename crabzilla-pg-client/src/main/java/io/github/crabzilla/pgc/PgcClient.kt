package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.EventBusPublisher
import io.github.crabzilla.stack.EventsPublisherOptions
import io.github.crabzilla.stack.EventsPublisherVerticle
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json

class PgcClient(val vertx: Vertx, val pgPool: PgPool, val json: Json) {

  /**
   * Creates a CommandController
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    saveCommandOption: Boolean
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(pgPool, json)
    val eventStore = PgcEventStore(config, pgPool, json, saveCommandOption)
    return CommandController(config.commandValidator, config.commandHandler, snapshotRepo, eventStore)
  }

  /**
   * Creates a CommandController with support to an PgcEventsProjector
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    saveCommandOption: Boolean,
    projector: PgcEventsProjector<E>
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(pgPool, json)
    val eventStore = PgcEventStore(config, pgPool, json, saveCommandOption, projector)
    return CommandController(config.commandValidator, config.commandHandler, snapshotRepo, eventStore)
  }

  /**
   * Creates a EventsPublisherVerticle
   */
  fun create(
    projection: String,
    options: EventsPublisherOptions
  ): EventsPublisherVerticle {
    val eventsScanner = PgcEventsScanner(pgPool, projection)
    // consider an additional optional param to filter events
    return EventsPublisherVerticle(
      eventsScanner,
      EventBusPublisher(options.targetEndpoint, vertx.eventBus()), options
    )
  }
}
