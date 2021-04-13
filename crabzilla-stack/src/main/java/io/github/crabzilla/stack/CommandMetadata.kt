package io.github.crabzilla.stack

import java.util.*

/**
 * The client must knows how to instantiate it.
 */
data class CommandMetadata(
    val aggregateRootId: Int,
    val id: UUID = UUID.randomUUID(),
    val causationId: UUID = id,
    val correlationID: UUID = id
)
