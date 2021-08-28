package io.github.crabzilla.example1

import io.github.crabzilla.engine.JsonContext
import io.github.crabzilla.engine.JsonContextProvider
import io.github.crabzilla.serder.KotlinSerDer
import io.github.crabzilla.serder.SerDer

class Example1JsonContextFactory : JsonContextProvider {
  override fun create(): JsonContext {
    return Example1JsonContext()
  }
  class Example1JsonContext : JsonContext {
    override fun serder(): SerDer {
      return KotlinSerDer(example1Json)
    }
  }
}
