package io.github.crabzilla.stack.command

import io.github.crabzilla.core.DomainState
import io.github.crabzilla.core.Snapshot
import io.vertx.core.Future
import java.util.UUID

/**
 * A repository for snapshots
 */
interface SnapshotRepository<A : DomainState> {
  fun get(id: UUID): Future<Snapshot<A>?>
}
