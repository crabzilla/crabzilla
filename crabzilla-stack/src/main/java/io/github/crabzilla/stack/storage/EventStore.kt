package io.github.crabzilla.stack.storage

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.Future

/**
 * An event store to append new events
 */
interface EventStore<A : DomainState, C : Command, E : DomainEvent> {
  fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void>
}
