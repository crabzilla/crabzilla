package io.github.crabzilla.internal

import io.github.crabzilla.Entity
import io.github.crabzilla.Snapshot
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface SnapshotRepository<E : Entity> {

  fun retrieve(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>)

  fun upsert(entityId: Int, snapshot: Snapshot<E>, aHandler: Handler<AsyncResult<Void>>)

}
