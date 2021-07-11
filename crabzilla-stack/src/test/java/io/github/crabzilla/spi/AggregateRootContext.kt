package io.github.crabzilla.spi

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandControllerConfig
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState

interface AggregateRootContext<A : DomainState, C : Command, E : DomainEvent> {
  fun config(): CommandControllerConfig<A, C, E>
}
