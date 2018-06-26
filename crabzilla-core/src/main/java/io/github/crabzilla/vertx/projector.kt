package io.github.crabzilla.vertx

import io.github.crabzilla.DomainEvent
import java.util.*

data class ProjectionData(val uowId: UUID, val uowSequence: Long, val targetId: String, val events: List<DomainEvent>)

