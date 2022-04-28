package io.github.crabzilla.kotlinx

import io.github.crabzilla.core.CommandComponent
import io.github.crabzilla.stack.JsonObjectSerDer
import io.vertx.core.json.JsonObject
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

class KotlinxJsonObjectSerDer<S: Any, C : Any, E : Any>(
  private val json: Json,
  commandComponent: CommandComponent<S, C, E>
): JsonObjectSerDer<S, C, E> {
  private val eventSerDer = PolymorphicSerializer(commandComponent.eventClass)
  private val commandSerDer = PolymorphicSerializer(commandComponent.commandClass)
  override fun eventFromJson(json: JsonObject): E {
    return this.json.decodeFromString(eventSerDer, json.toString())
  }
  override fun eventToJsonObject(event: E): JsonObject {
    return JsonObject(json.encodeToString(eventSerDer, event))
  }
  override fun commandToJsonObject(command: C) : JsonObject {
    return JsonObject(json.encodeToString(commandSerDer, command))
  }
}