package io.github.crabzilla.stream

data class TargetStream(
  private val stateType: String? = null,
  private val stateId: String? = null,
  val name: String = "$stateType@$stateId",
  val mustBeNew: Boolean = false,
) {
  fun stateType(): String {
    return stateType ?: name.split("@")[0]
  }

  fun stateId(): String {
    return stateId ?: name.split("@")[1]
  }
}
