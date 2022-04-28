package io.github.crabzilla.stack

import io.vertx.core.json.JsonObject

/**
 * A convention used is the property "type" withn JsonObject to figure out what is the type - polymorphism
 */
interface JsonObjectSerDer<S: Any, C: Any, E: Any> {
  fun eventFromJson(json: JsonObject): E
  fun eventToJsonObject(event: E): JsonObject
  fun commandToJsonObject(command: C): JsonObject
}