package io.github.crabzilla.stack

interface JsonContextProvider {
  fun create(): JsonContext
}
