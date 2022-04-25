package io.github.crabzilla.projection

import io.github.crabzilla.projection.EventbusTopicStrategy.GLOBAL
import io.github.crabzilla.projection.EventbusTopicStrategy.STATE_TYPE
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_PUBLISH
import io.github.crabzilla.projection.ProjectorStrategy.EVENTBUS_REQUEST_REPLY
import io.github.crabzilla.projection.ProjectorStrategy.POSTGRES_SAME_TRANSACTION
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class ProjectorConfig(
  val projectionName: String,
  val initialInterval: Long = DEFAULT_INITIAL_INTERVAL,
  val interval: Long = DEFAULT_INTERVAL,
  val maxNumberOfRows: Int = DEFAULT_NUMBER_ROWS,
  val maxInterval: Long = DEFAULT_MAX_INTERVAL,
  val metricsInterval: Long = DEFAULT_MAX_INTERVAL,
  val stateTypes: List<String> = listOf(),
  val eventTypes: List<String> = listOf(),
  val projectorStrategy: ProjectorStrategy = POSTGRES_SAME_TRANSACTION,
  val eventbusTopicStrategy: EventbusTopicStrategy = GLOBAL
) {
  companion object {
    private const val DEFAULT_INITIAL_INTERVAL = 15_000L
    private const val DEFAULT_INTERVAL = 5_000L
    private const val DEFAULT_NUMBER_ROWS = 250
    private const val DEFAULT_MAX_INTERVAL = 60_000L
    fun create(config: JsonObject): ProjectorConfig {
      val projectionName = config.getString("projectionName")
      val initialInterval = config.getLong("initialInterval", DEFAULT_INITIAL_INTERVAL)
      val interval = config.getLong("interval", DEFAULT_INTERVAL)
      val maxNumberOfRows = config.getInteger("maxNumberOfRows", DEFAULT_NUMBER_ROWS)
      val maxInterval = config.getLong("maxInterval", DEFAULT_INTERVAL)
      val metricsInterval = config.getLong("metricsInterval", DEFAULT_MAX_INTERVAL)
      val stateTypesArray = config.getJsonArray("stateTypes") ?: JsonArray()
      val stateTypes = stateTypesArray.iterator().asSequence().map { it.toString() }.toList()
      val eventTypesArray = config.getJsonArray("eventTypes") ?: JsonArray()
      val eventTypes = eventTypesArray.iterator().asSequence().map { it.toString() }.toList()
      val projectorStrategy = when (config.getString("projectorStrategy")) {
        "POSTGRES_SAME_TRANSACTION" -> POSTGRES_SAME_TRANSACTION
        "EVENTBUS_REQUEST_REPLY" -> EVENTBUS_REQUEST_REPLY
        "EVENTBUS_PUBLISH" -> EVENTBUS_PUBLISH
        else -> throw IllegalArgumentException("Invalid config for projectorStrategy")
      }
      val topicStrategy = when (config.getString("eventbusTopicStrategy")) {
        "GLOBAL" -> GLOBAL
        "STATE" -> STATE_TYPE
        else -> throw IllegalArgumentException("Invalid config for eventbusTopicStrategy")
      }
      return ProjectorConfig(
        projectionName = projectionName,
        initialInterval = initialInterval,
        interval = interval,
        maxInterval = maxInterval,
        metricsInterval = metricsInterval,
        maxNumberOfRows = maxNumberOfRows,
        stateTypes = stateTypes,
        eventTypes = eventTypes,
        projectorStrategy = projectorStrategy,
        eventbusTopicStrategy = topicStrategy
      )
    }
  }
}
