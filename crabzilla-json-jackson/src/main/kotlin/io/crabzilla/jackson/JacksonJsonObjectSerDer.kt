package io.crabzilla.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.crabzilla.context.JsonObjectSerDer
import io.vertx.core.json.JsonObject
import kotlin.reflect.KClass

class JacksonJsonObjectSerDer<T : Any>(
  private val json: ObjectMapper,
  private val clazz: KClass<T>,
) : JsonObjectSerDer<T> {
  override fun toJson(instance: T): JsonObject {
    return JsonObject(json.writeValueAsString(instance))
  }

  override fun fromJson(json: JsonObject): T {
    return this.json.readValue(json.toString(), clazz.java)
  }
}
