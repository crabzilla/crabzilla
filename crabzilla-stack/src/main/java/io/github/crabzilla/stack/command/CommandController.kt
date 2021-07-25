package io.github.crabzilla.stack.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.Future

/**
 * An event store to append new events
 */
interface CommandController<A : DomainState, C : Command, E : DomainEvent> {

  fun handle(metadata: CommandMetadata, command: C): Future<StatefulSession<A, E>>
}
