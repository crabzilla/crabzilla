package io.github.crabzilla.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.crabzilla.stack.JsonObjectSerDer
import io.github.crabzilla.stack.command.CommandServiceConfig
import io.vertx.core.json.JsonObject

class JacksonJsonObjectSerDer<S: Any, C: Any, E: Any>(
  private val json: ObjectMapper,
  private val featureComponent: CommandServiceConfig<S, C, E>) : JsonObjectSerDer<S, C, E> {
  override fun eventFromJson(json: JsonObject): E {
    return this.json.readValue(json.toString(), featureComponent.eventClass.java)
  }
  override fun eventToJson(event: E): JsonObject {
    return JsonObject(json.writeValueAsString(event))
  }
  override fun commandToJson(command: C): JsonObject {
    return JsonObject(json.writeValueAsString(command))
  }
  override fun commandFromJson(json: JsonObject): C {
    return this.json.readValue(json.toString(), featureComponent.commandClass.java)
  }
}
