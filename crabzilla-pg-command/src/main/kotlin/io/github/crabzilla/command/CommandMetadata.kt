package io.github.crabzilla.command

import io.vertx.core.json.JsonObject
import java.util.*

data class CommandMetadata(
  val commandId: UUID = UUID.randomUUID(),
  val metadata: JsonObject? = null,
)
