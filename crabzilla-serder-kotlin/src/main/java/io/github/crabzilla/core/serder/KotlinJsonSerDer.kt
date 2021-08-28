package io.github.crabzilla.core.serder

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

class KotlinJsonSerDer(private val json: Json) : JsonSerDer {

  companion object {
    private val cmdSerDer = PolymorphicSerializer(Command::class)
    private val eventSerDer = PolymorphicSerializer(Event::class)
    private val stateSerDer = PolymorphicSerializer(State::class)
  }

  // command

  override fun toJson(command: Command): String {
    return json.encodeToString(cmdSerDer, command)
  }

  override fun commandFromJson(asJson: String): Command {
    return json.decodeFromString(cmdSerDer, asJson)
  }

  // event

  override fun eventFromJson(asJson: String): Event {
    return json.decodeFromString(eventSerDer, asJson)
  }

  override fun toJson(event: Event): String {
    return json.encodeToString(eventSerDer, event)
  }

  // state

  override fun stateFromJson(asJson: String): State {
    return json.decodeFromString(stateSerDer, asJson)
  }

  override fun toJson(state: State): String {
    return json.encodeToString(stateSerDer, state)
  }
}
