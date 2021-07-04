package io.github.crabzilla.pgc

import kotlinx.serialization.json.Json

interface JsonContext {
  fun json(): Json
}
