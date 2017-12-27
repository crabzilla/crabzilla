package io.github.crabzilla.vertx.projection

import io.github.crabzilla.core.DomainEvent

data class EventProjectorTuple(val id: String, val event: DomainEvent)
