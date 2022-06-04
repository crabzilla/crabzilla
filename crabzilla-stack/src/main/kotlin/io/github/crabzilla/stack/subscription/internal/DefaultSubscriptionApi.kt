package io.github.crabzilla.stack.subscription.internal

import io.github.crabzilla.stack.subscription.SubscriptionApi
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

internal class DefaultSubscriptionApi(
  private val vertx: Vertx,
  private val subscriptionName: String,
  private val verticle: Verticle) : SubscriptionApi {

  private val endpoints: SubscriptionEndpoints = SubscriptionEndpoints(subscriptionName)
  private var isDeployed = false

  override fun name(): String {
    return subscriptionName
  }

  override fun deploy(deploymentOptions: DeploymentOptions): Future<String> {
    return vertx.deployVerticle(verticle, deploymentOptions)
      .onSuccess { isDeployed = true }
  }

  override fun isDeployed(): Boolean {
    return isDeployed
  }

  override fun pause(): Future<JsonObject> {
    return vertx.eventBus().request<JsonObject>(endpoints.pause(), null)
      .map { it.body() }
  }
  override fun resume(): Future<JsonObject> {
    return vertx.eventBus().request<JsonObject>(endpoints.resume(), null)
      .map { it.body() }
  }
  override fun status(): Future<JsonObject> {
    return vertx.eventBus().request<JsonObject>(endpoints.status(), null)
      .map { it.body() }
  }
  override fun handle(): Future<JsonObject> {
    return vertx.eventBus().request<JsonObject>(endpoints.handle(), null)
      .map { it.body() }
  }
}
