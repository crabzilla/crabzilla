package io.github.crabzilla.projection

interface JsonContextProvider {
  fun create(): JsonContext
}
