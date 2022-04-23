package io.github.crabzilla.core

data class CommandSessionData<S, E>(
  val originalState: S?,
  val events: List<E>,
  val newState: S
)
