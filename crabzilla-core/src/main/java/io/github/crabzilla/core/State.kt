package io.github.crabzilla.core

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
abstract class State {
  companion object {
    private val serDer = PolymorphicSerializer(State::class)
    fun <S : State> fromJson(json: Json, asJson: String): S {
      return json.decodeFromString(serDer, asJson) as S
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}
