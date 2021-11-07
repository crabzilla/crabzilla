package io.github.crabzilla.core.metadata

import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CommandId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.StateId
import java.util.UUID

data class CommandMetadata(
  val stateId: StateId,
  val commandId: CommandId = CommandId(UUID.randomUUID()),
  val correlationId: CorrelationId = CorrelationId(commandId.id),
  val causationId: CausationId = CausationId(commandId.id)
)
