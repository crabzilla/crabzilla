package io.github.crabzilla.internal

import io.github.crabzilla.UnitOfWork
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface UnitOfWorkJournal {

  fun append(unitOfWork: UnitOfWork, aHandler: Handler<AsyncResult<Long>>)
}
