package io.github.crabzilla.subscription

import CrabzillaContext
import EventProjector
import io.github.crabzilla.subscription.internal.DefaultSubscriptionApi
import io.github.crabzilla.subscription.internal.SubscriptionComponent
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.slf4j.LoggerFactory

class DefaultSubscriptionApiFactory(private val crabzilla: CrabzillaContext) : SubscriptionApiFactory {

  companion object {
    private val log = LoggerFactory.getLogger(DefaultSubscriptionApiFactory::class.java)
  }

  override fun subscription(config: SubscriptionConfig, eventProjector: EventProjector?): SubscriptionApi {
    log.info("Creating subscription ${config.subscriptionName}")
    val verticle = object : AbstractVerticle() {
      private lateinit var subscription: SubscriptionComponent
      override fun start(promise: Promise<Void>) {
        subscription = SubscriptionComponent(crabzilla, config, eventProjector)
        subscription.start()
          .onSuccess {
            log.info("Subscription [${config.subscriptionName}] started")
            promise.complete()
          }
          .onFailure { promise.fail(it) }
      }
    }
    return DefaultSubscriptionApi(crabzilla.vertx(), config.subscriptionName, verticle)
  }

}
