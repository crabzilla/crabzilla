package io.github.crabzilla.example1

import io.github.crabzilla.pgc.integration.JsonContext
import io.github.crabzilla.pgc.integration.JsonContextProvider
import kotlinx.serialization.json.Json

class Example1JsonFactory : JsonContextProvider {
  override fun create(): JsonContext {
    return DefaultJsonApi()
  }
  class DefaultJsonApi : JsonContext {
    override fun json(): Json {
      return customerJson
    }
  }
}
