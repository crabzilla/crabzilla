package io.github.crabzilla.pgc.engines

import kotlinx.serialization.json.Json

interface JsonApi {
  fun json(): Json
}
