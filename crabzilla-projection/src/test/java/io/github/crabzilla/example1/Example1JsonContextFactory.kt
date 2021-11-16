package io.github.crabzilla.example1

import io.github.crabzilla.command.JsonContext
import io.github.crabzilla.command.JsonContextProvider
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.core.json.KotlinJsonSerDer

class Example1JsonContextFactory : JsonContextProvider {
  override fun create(): JsonContext {
    return object : JsonContext {
      override fun get(): JsonSerDer {
        return KotlinJsonSerDer(example1Json)
      }
    }
  }
}
