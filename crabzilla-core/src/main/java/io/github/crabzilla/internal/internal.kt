package io.github.crabzilla.internal

import io.github.crabzilla.framework.UnitOfWork

fun fromUnitOfWork(uowId: Long, uow: UnitOfWork): UnitOfWorkEvents {
  return UnitOfWorkEvents(uowId, uow.entityId, uow.events)
}
