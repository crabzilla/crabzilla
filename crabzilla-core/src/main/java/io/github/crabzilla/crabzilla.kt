package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler

typealias CommandHandlerFactory<E> = (CommandMetadata, Command, Snapshot<E>,
                                      Handler<AsyncResult<UnitOfWork>>) -> EntityCommandHandler<E>
typealias Version = Int

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int,
                            val events: List<Pair<String, DomainEvent>>)

fun fromUnitOfWork(uowId: Long, uow: UnitOfWork) : UnitOfWorkEvents {
  return UnitOfWorkEvents(uowId, uow.entityId, uow.events)
}
