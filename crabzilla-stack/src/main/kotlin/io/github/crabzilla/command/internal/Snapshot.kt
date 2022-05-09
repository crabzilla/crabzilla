package io.github.crabzilla.command.internal

import java.util.UUID

internal data class Snapshot<S : Any>(val state: S, val version: Int, val causationId: UUID, val correlationId: UUID)
