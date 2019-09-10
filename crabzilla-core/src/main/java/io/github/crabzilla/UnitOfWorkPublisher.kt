package io.github.crabzilla

import io.github.crabzilla.framework.UnitOfWork
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

interface UnitOfWorkPublisher {

  fun publish(uow: UnitOfWork, uowId: Long, handler: Handler<AsyncResult<Void>>)

}
