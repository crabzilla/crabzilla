package io.github.crabzilla.context

import io.vertx.core.json.JsonObject

/**
 * A convention used is the property "type" within JsonObject to figure out what is the type - polymorphism
 */
interface JsonObjectSerDer<T : Any> {
  fun toJson(instance: T): JsonObject

  fun fromJson(json: JsonObject): T
}
