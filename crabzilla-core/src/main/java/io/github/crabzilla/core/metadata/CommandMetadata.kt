package io.github.crabzilla.core.metadata

import java.util.UUID

data class CommandMetadata(
  val stateId: UUID,
  val commandId: UUID = UUID.randomUUID(),
  val correlationId: UUID = commandId,
  val causationId: UUID = commandId
)
