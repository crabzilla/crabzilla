package io.github.crabzilla.core.entity

import java.beans.ConstructorProperties
import java.io.Serializable

data class Version @ConstructorProperties("valueAsLong")
constructor(val valueAsLong: Long) : Serializable {

  init {
    if (valueAsLong < 0) throw IllegalArgumentException("Version must be = zero or positive")
  }

  fun nextVersion(): Version {
    return Version(valueAsLong + 1)
  }

  companion object {

    var VERSION_ZERO = Version(0)

    fun create(version: Long): Version {
      return Version(version)
    }

    fun create(version: Int): Version {
      return Version(version.toLong())
    }
  }

}
