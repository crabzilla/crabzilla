package io.github.crabzilla.pgc

import kotlinx.serialization.json.Json

interface JsonApi {
  fun json(): Json
}
