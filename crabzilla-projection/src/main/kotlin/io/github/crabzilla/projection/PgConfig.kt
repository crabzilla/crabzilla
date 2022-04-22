package io.github.crabzilla.projection

interface PgConfig {
  fun username(): String
  fun password(): String
  fun url(): String
}
