package io.github.crabzilla.core.metadata

data class EventMetadata(
  val aggregateName: String,
  val stateId: Metadata.StateId,
  val eventId: Metadata.EventId,
  val correlationId: Metadata.CorrelationId,
  val causationId: Metadata.CausationId,
  val eventSequence: Long
)
