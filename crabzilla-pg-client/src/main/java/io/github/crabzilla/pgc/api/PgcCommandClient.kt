package io.github.crabzilla.pgc.api

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.PgcEventStore
import io.github.crabzilla.pgc.PgcEventsProjector
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.stack.CommandController

class PgcCommandClient(private val pgcClient: PgcClient) {

  /**
   * Creates a CommandController (agnostic about read model projections)
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    saveCommandOption: Boolean
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(pgcClient.sqlClient, pgcClient.json)
    val eventStore = PgcEventStore(config, pgcClient.pgPool, pgcClient.json, saveCommandOption)
    return CommandController(config, snapshotRepo, eventStore)
  }

  /**
   * Creates a CommandController with support to an PgcEventsProjector (sync read model projections)
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    saveCommandOption: Boolean,
    projector: PgcEventsProjector<E>
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(pgcClient.sqlClient, pgcClient.json)
    val eventStore = PgcEventStore(config, pgcClient.pgPool, pgcClient.json, saveCommandOption, projector)
    return CommandController(config, snapshotRepo, eventStore)
  }
}
