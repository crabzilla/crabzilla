package io.github.crabzilla.subscription


// TODO break into essential and infra
data class SubscriptionSpec(
  val subscriptionName: String,
  val stateTypes: List<String> = listOf(),
  val eventTypes: List<String> = listOf(),
  val imMemoryFilterFunction: ((List<String>, List<String>) -> Boolean)? = null
)

// TODO use strategy pattern for each kind of sinc: postgres or event bus
// TODO lock subscription operation using pg advisor
// TODO that observer to trigger an action given a projetion status is reached
// TODO consider to optionally plug a Json serder for events

// TODO this is infra
data class SubscriptionConfig(
  val subscriptionName: String,
  val initialInterval: Long = DEFAULT_INITIAL_INTERVAL,
  val interval: Long = DEFAULT_INTERVAL,
  val maxNumberOfRows: Int = DEFAULT_NUMBER_ROWS,
  val maxInterval: Long = DEFAULT_MAX_INTERVAL,
  val metricsInterval: Long = DEFAULT_MAX_INTERVAL,
  val stateTypes: List<String> = listOf(),
  val eventTypes: List<String> = listOf(),
  val sink: SubscriptionSink
) {
  companion object {
    private const val DEFAULT_INITIAL_INTERVAL = 15_000L
    private const val DEFAULT_INTERVAL = 5_000L
    private const val DEFAULT_NUMBER_ROWS = 250
    private const val DEFAULT_MAX_INTERVAL = 60_000L
  }
}
