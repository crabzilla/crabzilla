package io.github.crabzilla.engine

import io.github.crabzilla.core.serder.JsonSerDer

interface JsonContext {
  fun serder(): JsonSerDer
}
