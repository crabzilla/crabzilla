package io.github.crabzilla.core.entity

import java.beans.ConstructorProperties

data class Version @ConstructorProperties("valueAsLong")
constructor(val valueAsLong: Long) {

  init {
    if (valueAsLong < 0) throw IllegalArgumentException("Version must be = zero or positive")
  }

  fun nextVersion(): Version {
    return Version(valueAsLong + 1)
  }

}
