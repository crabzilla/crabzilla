package io.github.crabzilla.stack.subscription

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

interface SubscriptionApi {
  fun name(): String
  fun deploy(deploymentOptions: DeploymentOptions = DeploymentOptions().setInstances(1)): Future<String>
  fun isDeployed(): Boolean
  fun pause(): Future<JsonObject>
  fun resume(): Future<JsonObject>
  fun status(): Future<JsonObject>
  fun handle(): Future<JsonObject>
}
