package io.github.crabzilla.command

import io.vertx.core.json.JsonObject
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
    fun fromJsonObject(json: JsonObject) : CommandMetadata {
      return CommandMetadata(
        UUID.fromString(json.getString("stateId")),
        UUID.fromString(json.getString("correlationId")),
        UUID.fromString(json.getString("causationId")),
        UUID.fromString(json.getString("commandId")))
    }
  }
  fun toJsonObject() : JsonObject {
    return JsonObject()
      .put("stateId", stateId.toString())
      .put("correlationId", correlationId.toString())
      .put("causationId", causationId.toString())
      .put("commandId", commandId.toString())

  }
}
