package io.github.crabzilla.pgclient.command

interface JsonContextProvider {
  fun create(): JsonContext
}
