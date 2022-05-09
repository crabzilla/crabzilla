package io.github.crabzilla.command

import io.vertx.core.json.JsonObject
import java.util.UUID

data class CommandMetadata(val stateId: UUID, val causationId: UUID? = null) {
  companion object {
    fun new(stateId: UUID): CommandMetadata {
      return CommandMetadata(stateId, null)
    }
    fun new(stateId: UUID, causationId: UUID): CommandMetadata {
      return CommandMetadata(stateId, causationId)
    }
    fun fromJsonObject(json: JsonObject) : CommandMetadata {
      val correlationId = json.getString("correlationId")
      return CommandMetadata(
        UUID.fromString(json.getString("stateId")),
        if (correlationId == null) null else UUID.fromString(correlationId)
      )
    }
  }
  fun toJsonObject() : JsonObject {
    return JsonObject()
      .put("stateId", stateId.toString())
      .put("causationId", causationId.toString())
  }
}
