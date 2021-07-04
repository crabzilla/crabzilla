package io.github.crabzilla.spi

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent

interface AggregateRootContextProvider<A : AggregateRoot, C : Command, E : DomainEvent> {
  fun create(): AggregateRootContext<A, C, E>
}
