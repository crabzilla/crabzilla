package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.CommandController

class PgcCommandControllerFactory(private val commandControllerClient: PgcCommandControllerClient) {

  /**
   * Creates a CommandController (agnostic about read model projections)
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    saveCommandOption: Boolean
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(commandControllerClient.sqlClient, commandControllerClient.json)
    val eventStore = PgcEventStore(config, commandControllerClient.pgPool, commandControllerClient.json, saveCommandOption)
    return CommandController(config, snapshotRepo, eventStore)
  }

  /**
   * Creates a CommandController with support to an PgcEventsProjector (sync read model projections)
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    saveCommandOption: Boolean,
    projectorApi: PgcEventsProjectorApi
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(commandControllerClient.sqlClient, commandControllerClient.json)
    val eventStore = PgcEventStore(config, commandControllerClient.pgPool, commandControllerClient.json, saveCommandOption, projectorApi)
    return CommandController(config, snapshotRepo, eventStore)
  }
}
