package io.github.crabzilla.command

import io.github.crabzilla.core.metadata.EventMetadata

data class AppendedEvent<E>(val event: E, val metadata: EventMetadata)
