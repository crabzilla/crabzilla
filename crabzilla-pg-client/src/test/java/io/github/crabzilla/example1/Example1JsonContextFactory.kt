package io.github.crabzilla.example1

import io.github.crabzilla.core.serder.KotlinSerDer
import io.github.crabzilla.core.serder.SerDer
import io.github.crabzilla.pgc.JsonContext
import io.github.crabzilla.pgc.JsonContextProvider

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
