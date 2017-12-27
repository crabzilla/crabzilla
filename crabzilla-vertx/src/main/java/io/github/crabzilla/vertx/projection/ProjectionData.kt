package io.github.crabzilla.vertx.projection

import io.github.crabzilla.core.DomainEvent
import java.util.UUID

data class ProjectionData(val uowId: UUID, val uowSequence: Long?,
                          val targetId: String, val events: List<DomainEvent>)
