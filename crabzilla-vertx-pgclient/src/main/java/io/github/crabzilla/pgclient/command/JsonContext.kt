package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.json.JsonSerDer

interface JsonContext {
  fun get(): JsonSerDer
}
