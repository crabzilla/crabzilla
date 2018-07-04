package io.github.crabzilla.vertx

import io.github.crabzilla.DomainEvent
import io.vertx.core.Future
import java.util.*

data class ProjectionData(val uowId: UUID, val uowSequence: Long, val targetId: Int, val events: List<DomainEvent>)
