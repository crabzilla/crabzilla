package io.github.crabzilla.core.entity

import io.github.crabzilla.core.Version


data class Snapshot<out A : Entity>(val instance: A, val version: Version) {

  val isEmpty: Boolean
    get() = version.valueAsLong == 0L

  fun nextVersion(): Version {
    return version.nextVersion()
  }

}
