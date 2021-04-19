package io.github.crabzilla.cassandra

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.CommandController
import io.vertx.cassandra.CassandraClient

object CommandControllerFactory {

  fun <A : AggregateRoot, C : Command, E : DomainEvent>
  createPublishingTo(topic: String, config: AggregateRootConfig<A, C, E>, cassandra: CassandraClient):
    CommandController<A, C, E> {
      val snapshotRepo = CassandraSnapshotRepo(config, cassandra)
      val eventStore = CassandraEventStore(topic, cassandra, config)
      return CommandController(config.commandValidator, config.commandHandler, snapshotRepo, eventStore)
    }
}
