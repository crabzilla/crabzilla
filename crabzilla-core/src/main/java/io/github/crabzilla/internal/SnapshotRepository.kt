package io.github.crabzilla.internal

import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.Snapshot
import io.vertx.core.Promise

interface SnapshotRepository<E : Entity> {

  fun retrieve(entityId: Int) : Promise<Snapshot<E>>

  fun upsert(entityId: Int, snapshot: Snapshot<E>) : Promise<Void>

}
