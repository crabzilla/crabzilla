package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx

class EventBusUowPublisher(val vertx: Vertx, private val targetEndpoint: String): UnitOfWorkPublisher {

  override fun publish(uow: UnitOfWork, uowId: Long, handler: Handler<AsyncResult<Void>>) {
    vertx.eventBus().publish(targetEndpoint, UnitOfWorkEvents.fromUnitOfWork(uowId, uow))
    handler.handle(Future.succeededFuture())
  }

}
