package io.github.crabzilla.spi

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState

interface AggregateRootContextProvider<A : DomainState, C : Command, E : DomainEvent> {
  fun create(): AggregateRootContext<A, C, E>
}
