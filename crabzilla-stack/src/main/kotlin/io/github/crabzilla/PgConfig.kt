package io.github.crabzilla

interface PgConfig {
  fun username(): String
  fun password(): String
  fun url(): String
}
