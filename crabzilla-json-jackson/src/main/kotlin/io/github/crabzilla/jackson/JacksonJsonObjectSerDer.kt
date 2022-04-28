package io.github.crabzilla.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.core.CommandComponent
import io.github.crabzilla.stack.JsonObjectSerDer
import io.vertx.core.json.JsonObject

class JacksonJsonObjectSerDer<S: Any, C: Any, E: Any>(
  private val json: ObjectMapper,
  private val commandComponent: CommandComponent<S, C, E>) : JsonObjectSerDer<S, C, E> {
  override fun eventFromJson(json: JsonObject): E {
    return this.json.readValue(json.toString(), commandComponent.eventClass.java)
  }
  override fun eventToJsonObject(event: E): JsonObject {
    return JsonObject(json.writeValueAsString(event))
  }
  override fun commandToJsonObject(command: C): JsonObject {
    return JsonObject(json.writeValueAsString(command))
  }
}
