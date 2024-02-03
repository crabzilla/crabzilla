package io.github.crabzilla.subscription

import io.github.crabzilla.context.EventRecord
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

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

data class SubscriptionConfig(
  val initialInterval: Long = DEFAULT_INITIAL_INTERVAL,
  val interval: Long = DEFAULT_INTERVAL,
  val maxNumberOfRows: Int = DEFAULT_NUMBER_ROWS,
  val maxInterval: Long = DEFAULT_MAX_INTERVAL,
  val metricsInterval: Long = DEFAULT_MAX_INTERVAL,
) {
  companion object {
    private const val DEFAULT_INITIAL_INTERVAL = 15_000L
    private const val DEFAULT_INTERVAL = 5_000L
    private const val DEFAULT_NUMBER_ROWS = 250
    private const val DEFAULT_MAX_INTERVAL = 60_000L
  }
}

data class SubscriptionSpec(
  val subscriptionName: String,
  val stateTypes: List<String> = listOf(),
  val eventTypes: List<String> = listOf(),
  val discardEventIf: ((EventRecord) -> Boolean)? = { false },
)
