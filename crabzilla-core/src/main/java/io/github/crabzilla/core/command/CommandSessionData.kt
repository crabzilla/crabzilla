package io.github.crabzilla.core.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

data class CommandSessionData(
  val originalState: State?,
  val events: List<Event>,
  val newState: State
)
