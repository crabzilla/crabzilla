package io.github.crabzilla.stream

import java.util.*

data class StreamSnapshot<S : Any>(
  val streamId: Int,
  val state: S,
  val version: Int,
  val causationId: UUID?,
  val correlationId: UUID?,
)
