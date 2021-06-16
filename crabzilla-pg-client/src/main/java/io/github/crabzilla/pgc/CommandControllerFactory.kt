package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.CommandController
import io.vertx.pgclient.PgPool

object CommandControllerFactory {

  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    writeModelDb: PgPool,
    saveCommandOption: Boolean = true
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo(config, writeModelDb)
    val eventStore = PgcEventStore(config, writeModelDb, saveCommandOption)
    return CommandController(config.commandValidator, config.commandHandler, snapshotRepo, eventStore)
  }
}
