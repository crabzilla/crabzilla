package io.crabzilla.subscription

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.slf4j.LoggerFactory

internal class SubscriptionFactoryImpl : SubscriptionApiFactory {
  companion object {
    private val log = LoggerFactory.getLogger(SubscriptionApiImpl::class.java)
  }

  override fun create(subscriptionComponent: SubscriptionComponentImpl): SubscriptionApi {
    with(subscriptionComponent) {
      log.info("Creating subscription ${spec.subscriptionName}")
      val verticle =
        object : AbstractVerticle() {
          private lateinit var subscription: SubscriptionComponentImpl

          override fun start(promise: Promise<Void>) {
            subscription = subscriptionComponent
            subscription.start()
              .onSuccess {
                log.info("Subscription [${spec.subscriptionName}] started")
                promise.complete()
              }
              .onFailure { promise.fail(it) }
          }
        }
      return SubscriptionApiImpl(crabzillaContext.vertx, spec.subscriptionName, verticle)
    }
  }
}
