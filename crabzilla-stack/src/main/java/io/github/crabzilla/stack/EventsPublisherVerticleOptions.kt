package io.github.crabzilla.stack

import io.vertx.core.eventbus.EventBus

class EventsPublisherVerticleOptions private constructor(
  val targetEndpoint: String,
  val eventBus: EventBus,
  val interval: Long,
  val maxNumberOfRows: Int,
  val maxInterval: Long,
  val statsInterval: Long
) {
  data class Builder(
    private var targetEndpoint: String? = null,
    private var eventBus: EventBus? = null,
    private var intervalInMs: Long = 500L,
    private var maxNumberOfRows: Int = 500,
    private var maxIntervalInMs: Long = 60_000L,
    private var statsIntervalInMs: Long = 30_000L
  ) {
    fun targetEndpoint(v: String) = apply { this.targetEndpoint = v }
    fun eventBus(v: EventBus) = apply { this.eventBus = v }
    fun interval(v: Long) = apply { this.intervalInMs = v }
    fun maxNumberOfRows(v: Int) = apply { this.maxNumberOfRows = v }
    fun maxInterval(v: Long) = apply { this.maxIntervalInMs = v }
    fun statsInterval(v: Long) = apply { this.statsIntervalInMs = v }
    fun build() = EventsPublisherVerticleOptions(
      targetEndpoint!!, eventBus!!, intervalInMs,
      maxNumberOfRows, maxIntervalInMs, statsIntervalInMs
    )
  }
}
