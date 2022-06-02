package io.github.crabzilla.stack.command.internal

import java.util.*

internal data class Snapshot<S : Any>(val state: S, val version: Int, val causationId: UUID, val correlationId: UUID)
