package io.github.crabzilla.command

import java.util.UUID

data class CommandMetadata(val stateId: UUID, val versionValidation : ((Int) -> Boolean)? = null) {
  companion object {
    fun new(stateId: UUID): CommandMetadata {
      return CommandMetadata(stateId, null)
    }
    fun new(stateId: UUID, versionValidation : ((Int) -> Boolean)?): CommandMetadata {
      return CommandMetadata(stateId, versionValidation)
    }
  }
}
