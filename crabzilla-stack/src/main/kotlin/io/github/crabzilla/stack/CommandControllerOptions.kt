package io.github.crabzilla.stack

data class CommandControllerOptions(
  val eventProjector: EventProjector? = null,
  val eventBusTopic: String? = null,
  val eventStreamSize: Int = DEFAULT_EVENT_STREAM_SIZE,
  val pgNotificationInterval: Long = DEFAULT_NOTIFICATION_INTERVAL
) {
  companion object {
    private const val DEFAULT_NOTIFICATION_INTERVAL = 3000L
    private const val DEFAULT_EVENT_STREAM_SIZE = 1000
  }
}
