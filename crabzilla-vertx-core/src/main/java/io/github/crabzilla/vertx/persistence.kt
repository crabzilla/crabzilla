package io.github.crabzilla.vertx

import io.github.crabzilla.SnapshotData
import io.github.crabzilla.UnitOfWork
import io.github.crabzilla.Version
import io.vertx.core.Future
import java.util.*

interface UnitOfWorkRepository {

  fun getUowByCmdId(cmdId: UUID, uowFuture: Future<UnitOfWork>)

  fun getUowByUowId(uowId: UUID, uowFuture: Future<UnitOfWork>)

  operator fun get(query: String, id: UUID, uowFuture: Future<UnitOfWork>)

  fun selectAfterVersion(id: String, version: Version, selectAfterVersionFuture: Future<SnapshotData>,
                         aggregateRootName: String)

  fun append(unitOfWork: UnitOfWork, appendFuture: Future<Long>, aggregateRootName: String)

  fun selectAfterUowSequence(uowSequence: Long, maxRows: Int,
                             selectAfterUowSeq: Future<List<ProjectionData>>)
}

class DbConcurrencyException(s: String) : RuntimeException(s)
