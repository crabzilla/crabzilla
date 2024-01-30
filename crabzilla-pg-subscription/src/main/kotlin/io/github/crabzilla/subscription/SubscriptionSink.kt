package io.github.crabzilla.subscription

enum class SubscriptionSink {
  POSTGRES_PROJECTOR,
  EVENTBUS_PUBLISH,
  EVENTBUS_REQUEST_REPLY,
  EVENTBUS_REQUEST_REPLY_BLOCKING
}
