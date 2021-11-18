package io.github.crabzilla.core.metadata

import java.util.UUID

object Metadata {

  @JvmInline
  value class StateId(val id: UUID)

  @JvmInline
  value class CommandId(val id: UUID = UUID.randomUUID())

  @JvmInline
  value class EventId(val id: UUID)

  @JvmInline
  value class CorrelationId(val id: UUID)

  @JvmInline
  value class CausationId(val id: UUID)
}
