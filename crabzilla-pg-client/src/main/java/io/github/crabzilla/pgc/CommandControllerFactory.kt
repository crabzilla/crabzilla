package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandController
import io.github.crabzilla.core.DomainEvent
import io.vertx.pgclient.PgPool

object CommandControllerFactory {

  fun <A : AggregateRoot, C : Command, E : DomainEvent>
  create(config: AggregateRootConfig<A, C, E>, writeModelDb: PgPool): CommandController<A, C, E> {
    val snapshotRepo =
      PgcSnapshotRepo<A, C, E>(
        config.eventHandler, config.name.value, config.snapshotTableName.value,
        writeModelDb, config.json
      )
    val eventStore = PgcEventStore<A, C, E>(config.name.value, writeModelDb, config.json)
    return CommandController(config.commandValidator, config.commandHandler, snapshotRepo, eventStore)
  }
}
