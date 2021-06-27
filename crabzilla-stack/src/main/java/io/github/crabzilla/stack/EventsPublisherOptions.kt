package io.github.crabzilla.stack

data class EventsPublisherOptions private constructor(
  val targetEndpoint: String,
  val interval: Long,
  val maxNumberOfRows: Int,
  val maxInterval: Long,
  val statsInterval: Long
) {
  data class Builder(
    private var targetEndpoint: String? = null,
    private var intervalInMs: Long = 500L,
    private var maxNumberOfRows: Int = 500,
    private var maxIntervalInMs: Long = 60_000L,
    private var statsIntervalInMs: Long = 30_000L
  ) {
    fun targetEndpoint(v: String) = apply { this.targetEndpoint = v }
    fun interval(v: Long) = apply { this.intervalInMs = v }
    fun maxNumberOfRows(v: Int) = apply { this.maxNumberOfRows = v }
    fun maxInterval(v: Long) = apply { this.maxIntervalInMs = v }
    fun statsInterval(v: Long) = apply { this.statsIntervalInMs = v }
    fun build() = EventsPublisherOptions(
      targetEndpoint!!, intervalInMs,
      maxNumberOfRows, maxIntervalInMs, statsIntervalInMs
    )
  }
}
