package io.github.crabzilla.vertx.entity

import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.SnapshotData
import io.github.crabzilla.core.entity.Version
import io.github.crabzilla.vertx.projection.ProjectionData
import io.vertx.core.Future
import java.util.*

interface EntityUnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, uowFuture: Future<EntityUnitOfWork>)

  fun getUowByUowId(uowId: UUID, uowFuture: Future<EntityUnitOfWork>)

  operator fun get(querie: String, id: UUID, uowFuture: Future<EntityUnitOfWork>)

  fun selectAfterVersion(id: String, version: Version, selectAfterVersionFuture: Future<SnapshotData>,
                         aggregateRootName: String)

  fun append(unitOfWork: EntityUnitOfWork, appendFuture: Future<Long>, aggregateRootName: String)

  fun selectAfterUowSequence(uowSequence: Long, maxRows: Int,
                             selectAfterUowSeq: Future<List<ProjectionData>>)
}
