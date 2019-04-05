package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface SnapshotRepository<A : Entity> {

  fun retrieve(id: Int, entityName: String, aHandler: Handler<AsyncResult<Snapshot<A>>>)

}
