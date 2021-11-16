package io.github.crabzilla.command

import io.github.crabzilla.core.json.JsonSerDer

interface JsonContext {
  fun get(): JsonSerDer
}
