package io.github.crabzilla.pgc.integration

import kotlinx.serialization.json.Json

interface JsonContext {
  fun json(): Json
}
