package io.github.crabzilla.stack.command

import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.stack.CausationId
import io.github.crabzilla.stack.CommandId
import io.github.crabzilla.stack.CorrelationId
import io.github.crabzilla.stack.StateId
import io.vertx.core.json.JsonObject
import java.util.UUID

data class CommandMetadata(
  val stateId: StateId,
  val commandId: CommandId = CommandId(UuidCreator.getTimeOrdered()),
  val correlationId: CorrelationId = CorrelationId(commandId.id),
  val causationId: CausationId = CausationId(commandId.id)
) {
  companion object {
    fun fromJson(json: JsonObject): CommandMetadata {
      return CommandMetadata(
        StateId(UUID.fromString(json.getString("stateId"))),
        CommandId(UUID.fromString(json.getString("commandId"))),
        CorrelationId(UUID.fromString(json.getString("correlationId"))),
        CausationId(UUID.fromString(json.getString("causationId")))
      )
    }
  }
  fun toJson(): JsonObject {
    return JsonObject()
      .put("stateId", stateId.id.toString())
      .put("commandId", commandId.id.toString())
      .put("correlationId", correlationId.id.toString())
      .put("causationId", causationId.id.toString())
  }
}
