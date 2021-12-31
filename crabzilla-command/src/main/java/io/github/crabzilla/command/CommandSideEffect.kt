package io.github.crabzilla.command

import io.github.crabzilla.core.Event

data class CommandSideEffect<E : Event>(
  val appendedEvents: List<AppendedEvent<E>>,
  val resultingVersion: Int
)
