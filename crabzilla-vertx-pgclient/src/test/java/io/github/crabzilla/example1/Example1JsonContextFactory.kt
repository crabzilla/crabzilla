package io.github.crabzilla.example1

import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.command.JsonContext
import io.github.crabzilla.pgclient.command.JsonContextProvider

class Example1JsonContextFactory : JsonContextProvider {
  override fun create(): JsonContext {
    return object : JsonContext {
      override fun get(): JsonSerDer {
        return KotlinJsonSerDer(example1Json)
      }
    }
  }
}
