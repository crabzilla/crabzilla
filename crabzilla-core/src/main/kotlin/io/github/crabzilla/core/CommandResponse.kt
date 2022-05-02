package io.github.crabzilla.core

data class CommandResponse<S, E>(
  val originalState: S?,
  val events: List<E>,
  val newState: S
)
