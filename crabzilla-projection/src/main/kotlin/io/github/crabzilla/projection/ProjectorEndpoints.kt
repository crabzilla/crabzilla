package io.github.crabzilla.projection

data class ProjectorEndpoints(private val name: String) {
  fun status() = "crabzilla.verticles.$name.status"
  fun work() = "crabzilla.verticles.$name.handle"
  fun pause() = "crabzilla.verticles.$name.pause"
  fun resume() = "crabzilla.verticles.$name.resume"
}
