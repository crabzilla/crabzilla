package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface EntityComponent<E: Entity> {

  fun getUowByUowId(uowId: Long, aHandler: Handler<AsyncResult<UnitOfWork>>)

  fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>)

  fun getSnapshot(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>)

}
