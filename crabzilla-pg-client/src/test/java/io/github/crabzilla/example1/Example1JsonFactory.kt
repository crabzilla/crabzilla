package io.github.crabzilla.example1

import io.github.crabzilla.pgc.JsonApi
import io.github.crabzilla.pgc.JsonApiProvider
import kotlinx.serialization.json.Json

class Example1JsonFactory : JsonApiProvider {
  override fun create(): JsonApi {
    return DefaultJsonApi()
  }
  class DefaultJsonApi : JsonApi {
    override fun json(): Json {
      return customerJson
    }
  }
}
