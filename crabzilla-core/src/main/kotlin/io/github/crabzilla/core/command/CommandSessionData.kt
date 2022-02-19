package io.github.crabzilla.core.command

data class CommandSessionData<S, E>(
  val originalState: S?,
  val events: List<E>,
  val newState: S
)
