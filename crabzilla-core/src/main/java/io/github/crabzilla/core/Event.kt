package io.github.crabzilla.core

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
abstract class Event {
  companion object {
    private val serDer = PolymorphicSerializer(Event::class)
    fun <E : Event> fromJson(json: Json, asJson: String): E {
      return json.decodeFromString(serDer, asJson) as E
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}
