package io.github.crabzilla.core

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
abstract class DomainEvent {
  companion object {
    private val serDer = PolymorphicSerializer(DomainEvent::class)
    fun <D : DomainEvent> fromJson(json: Json, asJson: String): D {
      return json.decodeFromString(serDer, asJson) as D
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}
