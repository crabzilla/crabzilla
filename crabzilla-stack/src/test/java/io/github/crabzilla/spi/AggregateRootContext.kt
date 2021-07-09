package io.github.crabzilla.spi

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.stack.command.CommandControllerConfig

interface AggregateRootContext<A : DomainState, C : Command, E : DomainEvent> {
  fun config(): CommandControllerConfig<A, C, E>
}
