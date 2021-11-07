package io.github.crabzilla.engine.command

import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CommandId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.vertx.core.json.JsonObject
import java.util.UUID

object CommandMetadataExt {
  fun fromJson(json: JsonObject): CommandMetadata {
    return CommandMetadata(
      StateId(UUID.fromString(json.getString("stateId"))),
      CommandId(UUID.fromString(json.getString("commandId"))),
      CorrelationId(UUID.fromString(json.getString("correlationId"))),
      CausationId(UUID.fromString(json.getString("causationId")))
    )
  }
  fun CommandMetadata.toJson(): JsonObject {
    return JsonObject()
      .put("stateId", stateId.id.toString())
      .put("commandId", commandId.id.toString())
      .put("correlationId", correlationId.id.toString())
      .put("causationId", causationId.id.toString())
  }
}
