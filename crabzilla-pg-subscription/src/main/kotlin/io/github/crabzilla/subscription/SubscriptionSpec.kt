package io.github.crabzilla.subscription

import io.github.crabzilla.context.EventRecord

data class SubscriptionSpec(
  val subscriptionName: String,
  val stateTypes: List<String> = listOf(),
  val eventTypes: List<String> = listOf(),
  val discardEventIf: ((EventRecord) -> Boolean)? = { false },
)
