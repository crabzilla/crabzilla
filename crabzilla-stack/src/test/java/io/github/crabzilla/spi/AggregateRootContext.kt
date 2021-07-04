package io.github.crabzilla.spi

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.stack.AggregateRootConfig

interface AggregateRootContext<A : AggregateRoot, C : Command, E : DomainEvent> {
  fun config(): AggregateRootConfig<A, C, E>
}
