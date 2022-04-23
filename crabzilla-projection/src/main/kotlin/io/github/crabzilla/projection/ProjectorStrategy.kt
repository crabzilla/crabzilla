package io.github.crabzilla.projection

enum class ProjectorStrategy {
  EVENTBUS_PUBLISH,
  EVENTBUS_REQUEST_REPLY,
  POSTGRES_SAME_TRANSACTION
}
