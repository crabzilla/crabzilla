package io.github.crabzilla.vertx.projection

import io.github.crabzilla.core.DomainEvent

data class TargetIDDomainEventPair(val id: String, val event: DomainEvent)
