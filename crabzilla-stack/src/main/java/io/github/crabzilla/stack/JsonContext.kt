package io.github.crabzilla.stack

import io.github.crabzilla.core.serder.JsonSerDer

interface JsonContext {
  fun get(): JsonSerDer
}
