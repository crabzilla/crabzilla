package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface UnitOfWorkJournal {

  fun append(unitOfWork: UnitOfWork, aHandler: Handler<AsyncResult<Int>>)
}
