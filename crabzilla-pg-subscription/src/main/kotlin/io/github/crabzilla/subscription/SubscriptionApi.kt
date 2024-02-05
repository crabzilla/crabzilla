package io.github.crabzilla.subscription

import io.github.crabzilla.context.CrabzillaRuntimeException
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.ViewTrigger
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import org.slf4j.LoggerFactory

// TODO subscription should work with queries against view: ViewSubscription vs EventSubscriptions

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
  val jitterFunction: () -> Int = { ((0..5).random() * 1000) },
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

interface SubscriptionApiViewEffect {
  fun handle(
    sqlConnection: SqlConnection,
    eventRecord: EventRecord,
  ): Future<JsonObject?>
}

class SubscriptionCantBeLockedException(message: String) : CrabzillaRuntimeException(message)

internal class EventRecordProjector(
  private val sqlConnection: SqlConnection,
  private val viewEffect: SubscriptionApiViewEffect,
  private val viewTrigger: ViewTrigger? = null,
) {
  fun handle(appendedEvents: List<EventRecord>): Future<JsonObject?> {
    logger.debug("Will project {} events", appendedEvents.size)
    val initialFuture = Future.succeededFuture<JsonObject?>()
    return appendedEvents.fold(
      initialFuture,
    ) { currentFuture: Future<JsonObject?>, appendedEvent: EventRecord ->
      currentFuture.compose {
        viewEffect.handle(sqlConnection, appendedEvent)
      }
    }.onSuccess { viewAsJson ->
      if (viewAsJson != null && viewTrigger != null) {
        if (viewTrigger.checkCondition(viewAsJson)) {
          viewTrigger.handleTrigger(sqlConnection, viewAsJson)
        }
      }
    }
  }
  companion object {
    private val logger = LoggerFactory.getLogger(EventRecordProjector::class::java.name)
  }
}
