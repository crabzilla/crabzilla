package io.github.crabzilla.postgres

import io.github.crabzilla.json.JsonSerDer

interface JsonContext {
  fun get(): JsonSerDer
}
