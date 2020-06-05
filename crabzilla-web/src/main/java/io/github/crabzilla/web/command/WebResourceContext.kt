package io.github.crabzilla.web.command

import io.github.crabzilla.core.command.Entity
import io.github.crabzilla.core.command.EntityCommandAware
import io.github.crabzilla.core.command.SnapshotRepository

class WebResourceContext<E : Entity>(
  val cmdTypeMap: Map<String, String>,
  val cmdAware: EntityCommandAware<E>,
  val snapshotRepo: SnapshotRepository<E>
)
