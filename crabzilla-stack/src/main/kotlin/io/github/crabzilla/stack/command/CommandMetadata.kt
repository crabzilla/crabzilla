package io.github.crabzilla.stack.command

import java.util.UUID

data class CommandMetadata(
  val stateId: UUID,
  val correlationId: UUID,
  val causationId: UUID,
  val commandId: UUID
) {
  companion object {
    fun new(stateId: UUID): CommandMetadata {
      val commandId = UUID.randomUUID()
      return CommandMetadata(stateId, commandId, commandId, commandId)
    }
    fun new(stateId: UUID, correlationId: UUID, causationId: UUID): CommandMetadata {
      return CommandMetadata(stateId, correlationId, causationId, UUID.randomUUID())
    }
  }
}
