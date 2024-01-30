package io.github.crabzilla.kotlinx

import io.github.crabzilla.context.JsonObjectSerDer
import io.vertx.core.json.JsonObject
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

class KotlinxSerDer<T : Any>(
  private val kotlinxJson: Json,
  clazz: KClass<T>,
) : JsonObjectSerDer<T> {
  private val serDer = PolymorphicSerializer(clazz)

  override fun toJson(instance: T): JsonObject {
    return JsonObject(kotlinxJson.encodeToString(serDer, instance))
  }

  override fun fromJson(json: JsonObject): T {
    return kotlinxJson.decodeFromString(serDer, json.toString())
  }
}
