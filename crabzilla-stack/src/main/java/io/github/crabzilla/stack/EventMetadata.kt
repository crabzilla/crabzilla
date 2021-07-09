package io.github.crabzilla.stack

data class EventMetadata(
  val aggregateName: String,
  val domainStateId: DomainStateId,
  val eventId: EventId,
  val correlationId: CorrelationId,
  val causationId: CausationId,
  val eventSequence: Long
)
