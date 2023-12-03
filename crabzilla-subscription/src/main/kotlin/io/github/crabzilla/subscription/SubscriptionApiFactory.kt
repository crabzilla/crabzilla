package io.github.crabzilla.subscription

import EventProjector

interface SubscriptionApiFactory {

  fun subscription(config: SubscriptionConfig, eventProjector: EventProjector? = null): SubscriptionApi

}
