package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandController
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.SnapshotRepository
import io.vertx.pgclient.PgPool

object CommandControllerFactory {

  fun <A : AggregateRoot, C : Command, E : DomainEvent>
  createPublishingTo(
    topic: String,
    config: AggregateRootConfig<A, C, E>,
    writeModelDb: PgPool
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo(config, writeModelDb)
    val eventStore = PgcEventStore<A, C, E>(topic, writeModelDb, config.json)
    return CommandController(config.commandValidator, config.commandHandler, snapshotRepo, eventStore)
  }

  fun <A : AggregateRoot, C : Command, E : DomainEvent>
  createPublishingTo(
    topic: String,
    config: AggregateRootConfig<A, C, E>,
    writeModelDb: PgPool,
    snapshotRepo: SnapshotRepository<A, C, E>
  ): CommandController<A, C, E> {
    val eventStore = PgcEventStore<A, C, E>(topic, writeModelDb, config.json)
    return CommandController(config.commandValidator, config.commandHandler, snapshotRepo, eventStore)
  }
}
