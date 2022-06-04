package io.github.crabzilla.stack.command

import io.github.crabzilla.stack.EventProjector

data class CommandServiceOptions(
  val eventProjector: EventProjector? = null,
  val eventBusTopic: String? = null,
  val eventStreamSize: Int = DEFAULT_EVENT_STREAM_SIZE,
  val persistCommands: Boolean = true
) {
  companion object {
    private const val DEFAULT_EVENT_STREAM_SIZE = 1000
  }
}
