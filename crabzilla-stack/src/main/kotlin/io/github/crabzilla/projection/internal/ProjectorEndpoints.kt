package io.github.crabzilla.projection.internal

internal data class ProjectorEndpoints(val name: String) {
  fun status() = "crabzilla.verticle.$name.status"
  fun handle() = "crabzilla.verticle.$name.handle"
  fun pause() = "crabzilla.verticle.$name.pause"
  fun resume() = "crabzilla.verticle.$name.resume"
}
