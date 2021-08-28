package io.github.crabzilla.stack

import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

@JvmInline
value class StateId(val id: UUID)

@JvmInline
value class CommandId(val id: UUID = UuidCreator.getTimeOrdered())

@JvmInline
value class EventId(val id: UUID)

@JvmInline
value class CorrelationId(val id: UUID)

@JvmInline
value class CausationId(val id: UUID)
