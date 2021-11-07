package io.github.crabzilla.postgres

interface JsonContextProvider {
  fun create(): JsonContext
}
