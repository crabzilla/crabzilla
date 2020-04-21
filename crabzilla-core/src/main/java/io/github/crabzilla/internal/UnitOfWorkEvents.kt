package io.github.crabzilla.internal

import io.github.crabzilla.framework.DomainEvent

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int, val events: List<DomainEvent>)
