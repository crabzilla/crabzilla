package io.github.crabzilla.stack

interface PgConfig {
  fun username(): String
  fun password(): String
  fun url(): String
}
