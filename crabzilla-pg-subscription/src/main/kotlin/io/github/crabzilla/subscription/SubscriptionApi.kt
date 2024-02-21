package io.github.crabzilla.subscription

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

// TODO subscription should ALSO work with queries against view: ViewSubscription vs EventSubscriptions

interface SubscriptionComponent {
  fun extractApi(): SubscriptionApi
}

interface SubscriptionApiFactory {
  fun create(subscriptionComponent: SubscriptionComponentImpl): SubscriptionApi
}

interface SubscriptionApi {
  fun name(): String

  fun deploy(deploymentOptions: DeploymentOptions = DeploymentOptions().setInstances(1)): Future<String>

  fun isDeployed(): Boolean

  fun pause(): Future<JsonObject>

  fun resume(): Future<JsonObject>

  fun status(): Future<JsonObject>

  fun handle(): Future<JsonObject>
}

// TODO consider to optionally plug a Json serder for events
