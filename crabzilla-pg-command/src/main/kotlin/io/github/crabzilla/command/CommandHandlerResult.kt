package io.github.crabzilla.command

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.stream.StreamSnapshot

data class CommandHandlerResult<S : Any, E : Any>(
  val snapshot: StreamSnapshot<S>,
  val events: List<E>,
  val metadata: List<EventMetadata>,
)
