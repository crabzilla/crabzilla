package io.github.crabzilla.stack

import java.util.UUID

/**
 * The client must knows how to instantiate it.
 */
data class CommandMetadata(
  val aggregateRootId: AggregateRootId,
  val commandId: CommandId = CommandId(UUID.randomUUID()),
  val causationId: CommandId = commandId,
  val correlationId: CommandId = commandId
)
