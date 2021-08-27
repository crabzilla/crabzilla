package io.github.crabzilla.core.command

import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State

/**
 * To apply an event to an aggregate root state
 */
fun interface EventHandler<S : State, E : Event> {
  fun handleEvent(state: S?, event: E): S
}
