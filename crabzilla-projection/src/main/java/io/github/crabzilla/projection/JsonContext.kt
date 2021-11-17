package io.github.crabzilla.projection

import io.github.crabzilla.core.json.JsonSerDer

interface JsonContext {
  fun get(): JsonSerDer
}
