package io.github.crabzilla.pgc

interface JsonContextProvider {
  fun create(): JsonContext
}
