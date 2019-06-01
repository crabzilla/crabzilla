package io.github.crabzilla.internal

import io.github.crabzilla.Entity
import io.github.crabzilla.UnitOfWork
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface UnitOfWorkJournal<E: Entity> {

  fun append(unitOfWork: UnitOfWork, aHandler: Handler<AsyncResult<Long>>)
}
