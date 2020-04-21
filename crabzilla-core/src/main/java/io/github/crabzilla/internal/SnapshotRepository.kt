package io.github.crabzilla.internal

import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.Snapshot
import io.vertx.core.Future

interface SnapshotRepository<E : Entity> {
  fun retrieve(entityId: Int): Future<Snapshot<E>>
  fun upsert(entityId: Int, snapshot: Snapshot<E>): Future<Void>
}
