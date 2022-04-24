package io.github.crabzilla.stack

object CrabzillaConstants {

  const val POSTGRES_NOTIFICATION_CHANNEL = "crabzilla_channel"
  const val EVENTBUS_GLOBAL_TOPIC = "crabzilla.eventbus.global-topic"

  fun stateTypeTopic(stateType: String) = "crabzilla.eventbus.$stateType-topic"
}
