package io.github.crabzilla.core.metadata

import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.EventId
import io.github.crabzilla.core.metadata.Metadata.StateId

data class EventMetadata(
  val aggregateName: String,
  val stateId: StateId,
  val eventId: EventId,
  val correlationId: CorrelationId,
  val causationId: CausationId,
  val eventSequence: Long
)
