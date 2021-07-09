package io.github.crabzilla.stack

import java.util.UUID

@JvmInline
value class DomainStateId(val id: UUID)

@JvmInline
value class CommandId(val id: UUID = UUID.randomUUID())

@JvmInline
value class EventId(val id: UUID)

@JvmInline
value class CorrelationId(val id: UUID)

@JvmInline
value class CausationId(val id: UUID)
