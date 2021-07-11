package io.github.crabzilla.core

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
abstract class Command {
  companion object {
    private val serDer = PolymorphicSerializer(Command::class)
    fun <C : Command> fromJson(json: Json, asJson: String): C {
      return json.decodeFromString(serDer, asJson) as C
    }
  }
  fun toJson(json: Json): String {
    return json.encodeToString(serDer, this)
  }
}
