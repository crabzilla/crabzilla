package io.github.crabzilla.stack

import java.util.UUID

/**
 * The client must knows how to instantiate it.
 */
data class CommandMetadata(
  val aggregateRootId: AggregateRootId,
  val commandId: CommandId = CommandId(UUID.randomUUID()),
  val correlationId: CorrelationId = CorrelationId(commandId.id),
  val causationId: CausationId = CausationId(commandId.id)
)
