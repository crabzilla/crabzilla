package io.github.crabzilla.stack.command

import io.github.crabzilla.stack.projection.PgEventProjector

data class CommandControllerOptions(
  val pgEventProjector: PgEventProjector? = null,
  val publishToEventBus: Boolean = false, // TODO option to publish to stateType or to stream "all" or both
  // TODO in case of All, publish or request? (and fail if eventbus request fail?)
  val eventStreamSize: Int = DEFAULT_EVENT_STREAM_SIZE,
  val pgNotificationInterval: Long = DEFAULT_NOTIFICATION_INTERVAL
) {
  companion object {
    private const val DEFAULT_NOTIFICATION_INTERVAL = 3000L
    private const val DEFAULT_EVENT_STREAM_SIZE = 1000
  }
}
