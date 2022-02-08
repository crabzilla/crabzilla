package io.github.crabzilla.core.json

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

interface JsonSerDer {

  fun toJson(command: Command): String
  fun toJson(event: Event): String
  fun toJson(state: State): String

  fun commandFromJson(asJson: String): Command
  fun eventFromJson(asJson: String): Event
  fun stateFromJson(asJson: String): State
}
