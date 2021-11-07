package io.github.crabzilla.engine

import io.github.crabzilla.core.json.JsonSerDer

interface JsonContext {
  fun get(): JsonSerDer
}
