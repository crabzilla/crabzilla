package io.github.crabzilla.core.metadata

import java.util.UUID

data class EventMetadata(
  val stateType: String,
  val stateId: UUID,
  val eventId: UUID,
  val correlationId: UUID,
  val causationId: UUID,
  val eventSequence: Long
)
