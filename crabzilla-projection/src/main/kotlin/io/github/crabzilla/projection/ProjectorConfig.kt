package io.github.crabzilla.projection

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class ProjectorConfig(
  val projectionName: String,
  val viewName: String,
  val initialInterval: Long = 15_000,
  val interval: Long = 5_000,
  val maxNumberOfRows: Int = 250,
  val maxInterval: Long = 60_000,
  val metricsInterval: Long = 60_000,
  val stateTypes: List<String> = listOf(),
  val eventTypes: List<String> = listOf(),
) {
  companion object {
    fun create(config: JsonObject): ProjectorConfig {
      val projectionName = config.getString("projectionName")
      val viewName = config.getString("viewName")
      val initialInterval = config.getLong("initialInterval", 15_000)
      val interval = config.getLong("interval", 5_000)
      val maxNumberOfRows = config.getInteger("maxNumberOfRows", 250)
      val maxInterval = config.getLong("maxInterval", 60_000)
      val metricsInterval = config.getLong("metricsInterval", 60_000)
      val stateTypesArray = config.getJsonArray("stateTypes") ?: JsonArray()
      val stateTypes = stateTypesArray.iterator().asSequence().map { it.toString() }.toList()
      val eventTypesArray = config.getJsonArray("eventTypes") ?: JsonArray()
      val eventTypes = eventTypesArray.iterator().asSequence().map { it.toString() }.toList()
      return ProjectorConfig(
        projectionName = projectionName,
        viewName = viewName,
        initialInterval = initialInterval,
        interval = interval,
        maxInterval = maxInterval,
        metricsInterval = metricsInterval,
        maxNumberOfRows = maxNumberOfRows,
        stateTypes = stateTypes,
        eventTypes = eventTypes
      )
    }
  }
}
