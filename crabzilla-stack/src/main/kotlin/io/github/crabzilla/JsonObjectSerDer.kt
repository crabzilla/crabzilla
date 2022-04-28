package io.github.crabzilla

import io.vertx.core.json.JsonObject

/**
 * A convention used is the property "type" within JsonObject to figure out what is the type - polymorphism
 */
interface JsonObjectSerDer<S: Any, C: Any, E: Any> {
  fun eventFromJson(json: JsonObject): E
  fun eventToJson(event: E): JsonObject
  fun commandToJson(command: C): JsonObject
}