package io.github.crabzilla.vertx.entity

import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.SnapshotData
import io.github.crabzilla.core.entity.Version
import io.vertx.core.Future
import java.util.*

interface EntityUnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, uowFuture: Future<EntityUnitOfWork>)

  fun getUowByUowId(uowId: UUID, uowFuture: Future<EntityUnitOfWork>)

  operator fun get(querie: String, id: UUID, uowFuture: Future<EntityUnitOfWork>)

  fun selectAfterVersion(id: String, version: Version, selectAfterVersionFuture: Future<SnapshotData>)
  fun append(unitOfWork: EntityUnitOfWork, appendFuture: Future<Long>)
}
