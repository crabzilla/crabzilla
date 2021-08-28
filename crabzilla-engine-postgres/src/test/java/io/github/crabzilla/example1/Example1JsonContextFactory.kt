package io.github.crabzilla.example1

import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.core.serder.KotlinJsonSerDer
import io.github.crabzilla.engine.JsonContext
import io.github.crabzilla.engine.JsonContextProvider

class Example1JsonContextFactory : JsonContextProvider {
  override fun create(): JsonContext {
    return object : JsonContext {
      override fun serder(): JsonSerDer {
        return KotlinJsonSerDer(example1Json)
      }
    }
  }
}
