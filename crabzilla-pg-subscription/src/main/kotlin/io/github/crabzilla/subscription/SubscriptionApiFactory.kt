package io.github.crabzilla.subscription

import EventProjector
import jdk.jshell.JShell.Subscription

interface SubscriptionApiFactory {

  fun postgresSinkSubscription(spec: SubscriptionSpec,
                               config: SubscriptionConfig? = SubscriptionConfig(),
                               eventProjector: EventProjector)
  : SubscriptionApi

  fun eventbusSinkSubscription(spec: SubscriptionSpec,
                   config: SubscriptionConfig? = SubscriptionConfig())
  : SubscriptionApi

}
