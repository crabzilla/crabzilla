package io.github.crabzilla.stack.subscription

import io.github.crabzilla.stack.EventProjector

interface SubscriptionApiFactory {

  fun subscription(config: SubscriptionConfig, eventProjector: EventProjector? = null): SubscriptionApi

}
