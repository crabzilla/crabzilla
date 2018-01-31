package io.github.crabzilla.vertx

import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.vertx.core.Future
import java.util.*

interface EntityUnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, uowFuture: Future<EntityUnitOfWork>)

  fun getUowByUowId(uowId: UUID, uowFuture: Future<EntityUnitOfWork>)

  operator fun get(querie: String, id: UUID, uowFuture: Future<EntityUnitOfWork>)

}
