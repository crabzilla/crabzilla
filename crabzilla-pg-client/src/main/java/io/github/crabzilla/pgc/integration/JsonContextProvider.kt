package io.github.crabzilla.pgc.integration

interface JsonContextProvider {
  fun create(): JsonContext
}
