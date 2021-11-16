package io.github.crabzilla.command

interface JsonContextProvider {
  fun create(): JsonContext
}
