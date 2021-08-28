package io.github.crabzilla.engine

interface JsonContextProvider {
  fun create(): JsonContext
}
