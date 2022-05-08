package io.github.crabzilla.subscription

import io.github.crabzilla.subscription.internal.SubscriptionEndpoints
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject

class SubscriptionApi(private val bus: EventBus, subscriptionName: String) {
  private val endpoints: SubscriptionEndpoints = SubscriptionEndpoints(subscriptionName)
  fun pause(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.pause(), null)
      .map{ it.body() }
  }
  fun resume(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.resume(), null)
      .map{ it.body() }
  }
  fun status(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.status(), null)
      .map{ it.body() }
  }
  fun handle(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.handle(), null)
      .map{ it.body() }
  }
}