package io.github.crabzilla.subscription

enum class EventBusStrategy {
  EVENTBUS_PUBLISH,
  EVENTBUS_REQUEST_REPLY,
  EVENTBUS_REQUEST_REPLY_BLOCKING
}
