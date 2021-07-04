package io.github.crabzilla.example1

import io.github.crabzilla.pgc.JsonContext
import io.github.crabzilla.pgc.JsonContextProvider
import kotlinx.serialization.json.Json

class Example1JsonContextFactory : JsonContextProvider {
  override fun create(): JsonContext {
    return Example1JsonContext()
  }
  class Example1JsonContext : JsonContext {
    override fun json(): Json {
      return customerJson
    }
  }
}
