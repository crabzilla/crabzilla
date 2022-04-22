package io.github.crabzilla.projection

data class ProjectorEndpoints(private val name: String) {
  fun status() = "crabzilla.projectors.$name.status"
  fun work() = "crabzilla.projectors.$name.handle"
  fun pause() = "crabzilla.projectors.$name.pause"
  fun resume() = "crabzilla.projectors.$name.resume"
}
