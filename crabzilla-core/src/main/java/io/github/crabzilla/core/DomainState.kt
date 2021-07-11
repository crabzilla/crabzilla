package io.github.crabzilla.core

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
abstract class DomainState {
  companion object {
    private val serDer = PolymorphicSerializer(DomainState::class)
    fun <A : DomainState> fromJson(json: Json, asJson: String): A {
      return json.decodeFromString(serDer, asJson) as A
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}
