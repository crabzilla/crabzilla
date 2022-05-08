package io.github.crabzilla.projection

data class ProjectorConfig(
  val projectionName: String,
  val initialInterval: Long = DEFAULT_INITIAL_INTERVAL,
  val interval: Long = DEFAULT_INTERVAL,
  val maxNumberOfRows: Int = DEFAULT_NUMBER_ROWS,
  val maxInterval: Long = DEFAULT_MAX_INTERVAL,
  val metricsInterval: Long = DEFAULT_MAX_INTERVAL,
  val stateTypes: List<String> = listOf(),
  val eventTypes: List<String> = listOf(),
  val eventBusStrategy: EventBusStrategy? = null
) {
  companion object {
    private const val DEFAULT_INITIAL_INTERVAL = 15_000L
    private const val DEFAULT_INTERVAL = 5_000L
    private const val DEFAULT_NUMBER_ROWS = 250
    private const val DEFAULT_MAX_INTERVAL = 60_000L
  }
}
