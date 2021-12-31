package io.github.crabzilla.core.metadata

import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.EventId
import io.github.crabzilla.core.metadata.Metadata.StateId

data class EventMetadata(
  val aggregateName: String,
  @get:JvmName("getStateId")
  val stateId: StateId,
  @get:JvmName("getEventId")
  val eventId: EventId,
  @get:JvmName("getCorrelationId")
  val correlationId: CorrelationId,
  @get:JvmName("getCausationId")
  val causationId: CausationId,
  val eventSequence: Long
)
