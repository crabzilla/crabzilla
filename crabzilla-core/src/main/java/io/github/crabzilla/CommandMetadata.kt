package io.github.crabzilla

import java.util.*

data class CommandMetadata(val entityName: String, val entityId: Int, val commandName: String,
                           val commandId: UUID? = UUID.randomUUID())
