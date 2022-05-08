package io.github.crabzilla.projection

import io.github.crabzilla.projection.internal.ProjectorEndpoints
import io.vertx.core.Future
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject

class ProjectorApi(private val bus: EventBus, projectionName: String) {
  private val endpoints: ProjectorEndpoints = ProjectorEndpoints(projectionName)
  fun pause(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.pause(), null)
      .map{ it.body() }
  }
  fun resume(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.resume(), null)
      .map{ it.body() }
  }
  fun status(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.status(), null)
      .map{ it.body() }
  }
  fun handle(): Future<JsonObject> {
    return bus.request<JsonObject>(endpoints.handle(), null)
      .map{ it.body() }
  }
}