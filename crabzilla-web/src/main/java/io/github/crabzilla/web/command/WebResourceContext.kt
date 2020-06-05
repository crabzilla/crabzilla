package io.github.crabzilla.web.command

import io.github.crabzilla.core.command.AggregateRoot
import io.github.crabzilla.core.command.AggregateRootCommandAware
import io.github.crabzilla.core.command.SnapshotRepository

class WebResourceContext<A : AggregateRoot>(
  val cmdTypeMap: Map<String, String>,
  val cmdAware: AggregateRootCommandAware<A>,
  val snapshotRepo: SnapshotRepository<A>
)
