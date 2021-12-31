package io.github.crabzilla.core.metadata

import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CommandId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.StateId
import java.util.UUID

data class CommandMetadata(
  @get:JvmName("getStateId")
  val stateId: StateId,
  @get:JvmName("getCommandId")
  val commandId: CommandId = CommandId(UUID.randomUUID()),
  @get:JvmName("getCorrelationId")
  val correlationId: CorrelationId = CorrelationId(commandId.id),
  @get:JvmName("getCausationId")
  val causationId: CausationId = CausationId(commandId.id)
)
